package networthcalculator.http

import cats.Applicative
import cats.effect.Sync
import io.circe._
import io.circe.generic.semiauto._
import networthcalculator.domain.asset._
import networthcalculator.domain.auth.Role
import networthcalculator.domain.healthcheck.{AppStatus, PostgresStatus, RedisStatus}
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{
  CreateUser,
  EncryptedPassword,
  LoginUser,
  PasswordParam,
  Salt,
  UserId,
  UserName,
  UserNameParam,
  UserWithPassword
}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe._

object json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
  implicit def jsonDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]
}

private[http] trait JsonCodecs {

  implicit val usernameParamDecoder: Decoder[UserNameParam] = deriveDecoder
  implicit val passwordParamDecoder: Decoder[PasswordParam] = deriveDecoder

  implicit val assetTypeParamDecoder: Decoder[AssetTypeParam] = deriveDecoder
  implicit val assetIdParamDecoder: Decoder[AssetIdParam] = deriveDecoder

  implicit val userIdDecoder: Decoder[UserId] = deriveDecoder
  implicit val userNameDecoder: Decoder[UserName] = deriveDecoder
  implicit val passwordDecoder: Decoder[EncryptedPassword] = deriveDecoder
  implicit val saltDecoder: Decoder[Salt] = deriveDecoder
  implicit val roleDecoder: Decoder[Role] = deriveDecoder
  implicit val userIdEncoder: Encoder[UserId] = deriveEncoder
  implicit val userNameEncoder: Encoder[UserName] = deriveEncoder
  implicit val passwordEncoder: Encoder[EncryptedPassword] = deriveEncoder
  implicit val saltEncoder: Encoder[Salt] = deriveEncoder
  implicit val roleEncoder: Encoder[Role] = deriveEncoder
  implicit val userDecoder: Decoder[UserWithPassword] = deriveDecoder
  implicit val userEncoder: Encoder[UserWithPassword] = deriveEncoder

  implicit val assetIdDecoder: Decoder[AssetId] = deriveDecoder
  implicit val assetTypeDecoder: Decoder[AssetType] = deriveDecoder
  implicit val assetIdEncoder: Encoder[AssetId] = deriveEncoder
  implicit val assetTypeEncoder: Encoder[AssetType] = deriveEncoder
  implicit val assetDecoder: Decoder[Asset] = deriveDecoder
  implicit val assetEncoder: Encoder[Asset] = deriveEncoder

  implicit val redisStatus: Encoder[RedisStatus] = deriveEncoder
  implicit val postgresStatusStatus: Encoder[PostgresStatus] = deriveEncoder
  implicit val appStatusEncoder: Encoder[AppStatus] = deriveEncoder

  implicit val createUserDecoder: Decoder[CreateUser] = deriveDecoder
  implicit val loginUserDecoder: Decoder[LoginUser] = deriveDecoder
  implicit val createAssetDecoder: Decoder[CreateAsset] = deriveDecoder
  implicit val updateAssetDecoder: Decoder[UpdateAsset] = deriveDecoder

  implicit val jwtTokenDecoder: Decoder[JwtToken] = deriveDecoder
  implicit val jwtTokenEncoder: Encoder[JwtToken] = deriveEncoder

}
