package networthcalculator.algebras

import networthcalculator.domain.asset._

trait Assets[F[_]] {
  def findAll: F[List[Asset]]
  def create(assetType: AssetType): F[Unit]
  def update(asset: Asset): F[Unit]
  def delete(assetId: AssetId): F[Unit]
}