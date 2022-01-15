package networthcalculator.algebras

import networthcalculator.domain.assets._
import networthcalculator.domain.users.UserId

trait AssetsService[F[_]] {
  def findAll(userId: UserId): F[List[Asset]]
  def create(assetType: AssetType, assetName: AssetName, userId: UserId): F[Unit]
  def delete(assetId: AssetId, userId: UserId): F[Unit]
}
