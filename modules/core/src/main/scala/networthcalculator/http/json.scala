package networthcalculator.http

import cats.Applicative
import io.circe._
import io.circe.generic.semiauto._
import io.circe.refined._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import networthcalculator.domain.asset._
import networthcalculator.domain.healthcheck.AppStatus
import networthcalculator.domain.users.{CreateUser, LoginUser, User, UserNameParam}
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

  // ----- Coercible codecs -----
  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])

  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.repr.asInstanceOf[B])

  implicit def coercibleKeyDecoder[A: Coercible[B, *], B: KeyDecoder]: KeyDecoder[A] =
    KeyDecoder[B].map(_.coerce[A])

  implicit def coercibleKeyEncoder[A: Coercible[B, *], B: KeyEncoder]: KeyEncoder[A] =
    KeyEncoder[B].contramap[A](_.repr.asInstanceOf[B])

  implicit val userDecoder: Decoder[User] = deriveDecoder[User]
  implicit val userEncoder: Encoder[User] = deriveEncoder[User]

  implicit val assetDecoder: Decoder[Asset] = deriveDecoder[Asset]
  implicit val assetEncoder: Encoder[Asset] = deriveEncoder[Asset]

  implicit val appStatusEncoder: Encoder[AppStatus] = deriveEncoder[AppStatus]

  implicit val createUserDecoder: Decoder[CreateUser] = deriveDecoder[CreateUser]
  implicit val loginUserDecoder: Decoder[LoginUser] = deriveDecoder[LoginUser]
  implicit val createAssetDecoder: Decoder[CreateAsset] = deriveDecoder[CreateAsset]
  implicit val updateAssetDecoder: Decoder[UpdateAsset] = deriveDecoder[UpdateAsset]

//  implicit val transactionDecoder: Decoder[Transaction] = deriveDecoder[Transaction]
//  implicit val transactionEncoder: Encoder[Transaction] = deriveEncoder[Transaction]
}
