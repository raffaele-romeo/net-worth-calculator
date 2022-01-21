package networthcalculator.domain

import doobie.util.{Read => DRead, Write => DWrite}
import io.circe._
import io.circe.generic.semiauto._
import networthcalculator.domain.users.UserId

import scala.util.control.NoStackTrace

object assets {

  opaque type AssetId = Long
  object AssetId {
    def apply(d: Long): AssetId = d

    given Decoder[AssetId] = Decoder.decodeLong
    given Encoder[AssetId] = Encoder.encodeLong
  }
  extension (x: AssetId) {
    def toLong: Long = x
  }

  opaque type AssetName = String
  object AssetName {
    def apply(d: String): AssetName = d

    given Decoder[AssetName] = Decoder.decodeString
    given Encoder[AssetName] = Encoder.encodeString
  }

  final case class Asset(
      assetId: AssetId,
      assetType: AssetType,
      assetName: AssetName,
      userId: UserId
  ) derives Encoder.AsObject

  object Asset {
    given DRead[Asset] =
      DRead[(Long, String, String, Long)].map { case (id, assetType, assetName, userId) =>
        Asset(
          AssetId(id),
          AssetType.make(assetType),
          AssetName(assetName.capitalize),
          UserId(userId)
        )
      }
    given DWrite[Asset] =
      DWrite[(Long, String, String, Long)].contramap { asset =>
        (
          asset.assetId.toLong,
          asset.assetType.toString,
          asset.assetName.toString,
          asset.userId.toLong
        )
      }
  }

  final case class CreateAsset(assetType: String, assetName: AssetName)

  enum AssetType {
    case Loan, Cash, Investment, Property
  }

  object AssetType {
    def make(s: String): AssetType = {
      AssetType.valueOf(s.toLowerCase.capitalize)
    }

    given Encoder[AssetType] = Encoder[String].contramap(_.toString)
  }

  final case class AssetTypeNotAllowed(error: String) extends NoStackTrace
  final case class AssetAlreadyInUse(error: String)   extends NoStackTrace
}
