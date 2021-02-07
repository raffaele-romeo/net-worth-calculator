package networthcalculator.algebras

import cats.effect._
import doobie._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import networthcalculator.domain.asset._
import networthcalculator.effects.BracketThrow
import cats.implicits._

trait Assets[F[_]] {
  def findAll: F[List[Asset]]
  def create(assetType: AssetType): F[Unit]
  def update(asset: Asset): F[Unit]
  def delete(assetId: AssetId): F[Unit]
}

object LiveAssets {

  def make[F[_]: Sync](transactor: Resource[F, HikariTransactor[F]]): F[LiveAssets[F]] = {
    Sync[F]
      .delay {
        new LiveAssets[F](transactor)
      }
  }
}

final class LiveAssets[F[_]: BracketThrow: Sync] private (
    transactor: Resource[F, HikariTransactor[F]]
) extends Assets[F] {

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

private object AssetsQueries {

  import networthcalculator.ext.CoercibleDoobieCodec._

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
