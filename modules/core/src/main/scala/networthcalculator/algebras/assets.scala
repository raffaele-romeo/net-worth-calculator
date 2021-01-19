package networthcalculator.algebras

import networthcalculator.domain.asset._

trait Assets[F[_]] {
  def findAll: F[List[Asset]]
  def create(accountType: AssetType): F[Unit]
  def update(account: Asset): F[Unit]
  def delete(accountId: AssetId): F[Unit]
}
