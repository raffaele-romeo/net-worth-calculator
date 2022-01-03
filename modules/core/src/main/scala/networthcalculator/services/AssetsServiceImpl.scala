package networthcalculator.services

import cats.effect.{Resource, MonadCancelThrow}
import cats.implicits.*
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import networthcalculator.algebras.AssetsService
import networthcalculator.domain.asset.{Asset, AssetId, AssetType, AssetTypeInUse}
import doobie.postgres.*

object AssetsServiceImpl {
  def make[F[_]: MonadCancelThrow](
      transactor: Resource[F, HikariTransactor[F]]
  ): AssetsService[F] = new AssetsService[F] {

    override def findAll: F[List[Asset]] =
      transactor
        .use(
          AssetsQueries.select
            .transact[F]
        )

    override def create(assetType: AssetType): F[Unit] =
      transactor
        .use(
          AssetsQueries
            .insert(assetType)
            .exceptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
              AssetTypeInUse(assetType).raiseError[ConnectionIO, Int]
            }
            .transact[F]
        )
        .void

    override def update(asset: Asset): F[Unit] =
      transactor
        .use(
          AssetsQueries
            .update(asset)
            .exceptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
              AssetTypeInUse(asset.assetType).raiseError[ConnectionIO, Int]
            }
            .transact[F]
        )
        .void

    override def delete(assetId: AssetId): F[Unit] =
      transactor
        .use(
          AssetsQueries
            .delete(assetId)
            .transact[F]
        )
        .void
  }
}

private object AssetsQueries {

  def insert(assetType: AssetType): ConnectionIO[Int] =
    sql"""
         | INSERT INTO assets (
         | name
         | )
         | VALUES (
         | ${assetType.name}
         | )
         """.stripMargin.update.run

  def select: ConnectionIO[List[Asset]] =
    sql"""
         | SELECT id, name
         | FROM ASSETS
         """.stripMargin.query[Asset].to[List]

  def update(asset: Asset): ConnectionIO[Int] =
    sql"""
         | UPDATE assets SET 
         | name = ${asset.assetType.name}
         | WHERE id = ${asset.assetId.value}
         """.stripMargin.update.run

  def delete(assetId: AssetId): ConnectionIO[Int] =
    sql"""
         | DELETE FROM assets
         | WHERE id = ${assetId.value}
         """.stripMargin.update.run
}
