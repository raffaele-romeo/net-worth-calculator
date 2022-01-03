package networthcalculator.domain

import scala.util.control.NoStackTrace

object asset {
  final case class AssetId(value: Long)
  final case class AssetType(name: String)

  final case class Asset(assetId: AssetId, assetType: AssetType)

  final case class CreateAsset(assetType: String) {
    def toDomain: AssetType = AssetType(assetType.toLowerCase())
  }

  final case class UpdateAsset(assetId: Long, assetType: String) {
    def toDomain: Asset = Asset(AssetId(assetId), AssetType(assetType.toLowerCase()))
  }

  final case class AssetTypeInUse(assetType: AssetType) extends NoStackTrace
}
