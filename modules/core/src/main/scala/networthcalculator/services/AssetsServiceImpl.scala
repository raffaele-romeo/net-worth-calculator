package networthcalculator.services

import cats.effect.{Resource, Sync}
import cats.implicits._
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import networthcalculator.algebras.AssetsService
import networthcalculator.domain.asset.{Asset, AssetId, AssetType}

object AssetsServiceImpl {
  def make[F[_]: Sync](
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
            .transact[F]
        )
        .void

    override def update(asset: Asset): F[Unit] =
      transactor
        .use(
          AssetsQueries
            .update(asset)
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
