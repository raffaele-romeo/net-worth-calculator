package networthcalculator.services

import cats.effect.MonadCancelThrow
import cats.implicits.*
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.postgres.*
import doobie.util.log.LogHandler
import networthcalculator.algebras.AssetsService
import networthcalculator.domain.assets.*
import networthcalculator.domain.users.UserId
import org.typelevel.log4cats.Logger

object AssetsServiceImpl:
  def make[F[_]: MonadCancelThrow](
    transactor: HikariTransactor[F]
  ): AssetsService[F] =
    new AssetsService[F]:

      override def findAll(userId: UserId): F[List[Asset]] =
        AssetsQueries
          .select(userId)
          .transact[F](transactor)

      override def create(
        asseType: AssetType,
        assetName: AssetName,
        userId: UserId
      ): F[Unit] =
        AssetsQueries
          .insert(asseType, assetName, userId)
          .exceptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
            AssetAlreadyInUse(
              s"Asset ${assetName.toString} - ${asseType.toString} already in use"
            ).raiseError
          }
          .transact[F](transactor)
          .void

      override def delete(assetId: AssetId, userId: UserId): F[Unit] =
        AssetsQueries
          .delete(assetId, userId)
          .transact[F](transactor)
          .void

private object AssetsQueries:

  def insert(
    assetType: AssetType,
    assetName: AssetName,
    userId: UserId
  ): ConnectionIO[Int] =
    sql"""
         | INSERT INTO assets (
         | asset_type,
         | asset_name,
         | user_id
         | )
         | VALUES (
         | ${assetType.toString.toLowerCase},
         | ${assetName.toString.toLowerCase},
         | ${userId.toLong}
         | )
         """.stripMargin.update.run

  def select(userId: UserId): ConnectionIO[List[Asset]] =
    sql"""
         | SELECT id, asset_type, asset_name, user_id
         | FROM assets
         | WHERE user_id = ${userId.toLong}
         """.stripMargin.query[Asset].to[List]

  def delete(assetId: AssetId, userId: UserId): ConnectionIO[Int] =
    sql"""
         | DELETE FROM assets
         | WHERE id = ${assetId.toLong} AND user_id = ${userId.toLong}
         """.stripMargin.update.run
