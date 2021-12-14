package networthcalculator.domain

import eu.timepit.refined.types.numeric.NonNegLong
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object asset {
  @newtype final case class AssetId(value: Long)
  @newtype final case class AssetType(name: String)

  case class Asset(assetId: AssetId, assetType: AssetType)

  @newtype case class AssetTypeParam(value: NonEmptyString) {
    def toDomain: AssetType = AssetType(value.value.toLowerCase())
  }

  @newtype case class AssetIdParam(value: NonNegLong) {
    def toDomain: AssetId = AssetId(value.value)
  }

  case class CreateAsset(assetType: AssetTypeParam)

  case class UpdateAsset(assetId: AssetIdParam, assetType: AssetTypeParam) {
    def toDomain: Asset = Asset(AssetId(assetId.value.value), AssetType(assetType.value.value.toLowerCase()))
  }
}
