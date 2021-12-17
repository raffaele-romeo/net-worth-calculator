package networthcalculator.http

import cats.Applicative
import io.circe._
import io.circe.generic.semiauto._
import networthcalculator.domain.asset._
import networthcalculator.domain.healthcheck.AppStatus
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{CreateUser, LoginUser, UserNameParam, UserWithPassword}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

object json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}

private[http] trait JsonCodecs {

  // ----- Overriding some Coercible codecs ----
  implicit val usernameParamDecoder: Decoder[UserNameParam] =
    Decoder.forProduct1("name")(UserNameParam.apply)

  implicit val assetTypeParamDecoder: Decoder[AssetTypeParam] =
    Decoder.forProduct1("name")(AssetTypeParam.apply)

  implicit val assetIdParamDecoder: Decoder[AssetIdParam] =
    Decoder.forProduct1("name")(AssetIdParam.apply)

  implicit val userDecoder: Decoder[UserWithPassword] = deriveDecoder[UserWithPassword]
  implicit val userEncoder: Encoder[UserWithPassword] = deriveEncoder[UserWithPassword]

  implicit val assetDecoder: Decoder[Asset] = deriveDecoder[Asset]
  implicit val assetEncoder: Encoder[Asset] = deriveEncoder[Asset]

  implicit val appStatusEncoder: Encoder[AppStatus] = deriveEncoder[AppStatus]

  implicit val createUserDecoder: Decoder[CreateUser] = deriveDecoder[CreateUser]
  implicit val loginUserDecoder: Decoder[LoginUser] = deriveDecoder[LoginUser]
  implicit val createAssetDecoder: Decoder[CreateAsset] = deriveDecoder[CreateAsset]
  implicit val updateAssetDecoder: Decoder[UpdateAsset] = deriveDecoder[UpdateAsset]

  implicit val jwtTokenDecoder: Decoder[JwtToken] = deriveDecoder[JwtToken]
  implicit val jwtTokenEncoder: Encoder[JwtToken] = deriveEncoder[JwtToken]

}
