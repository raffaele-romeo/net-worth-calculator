package networthcalculator.domain

object asset {
  final case class AssetId(value: Long)
  final case class AssetType(name: String)

  final case class Asset(assetId: AssetId, assetType: AssetType)

  final case class AssetTypeParam(value: String) {
    def toDomain: AssetType = AssetType(value.toLowerCase())
  }

  final case class AssetIdParam(value: Long) {
    def toDomain: AssetId = AssetId(value)
  }

  final case class CreateAsset(assetType: AssetTypeParam)

  final case class UpdateAsset(assetId: AssetIdParam, assetType: AssetTypeParam) {
    def toDomain: Asset = Asset(AssetId(assetId.value), AssetType(assetType.value.toLowerCase()))
  }
}
