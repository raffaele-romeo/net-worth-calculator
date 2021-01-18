package networthcalculator.http

import cats.Applicative
import io.circe.generic.semiauto._
import io.circe.{ Encoder, _ }
import io.circe.refined._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import networthcalculator.domain.auth.{ CreateUser, User }
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

object json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}

private[http] trait JsonCodecs {

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

  implicit val createUserDecoder: Decoder[CreateUser] = deriveDecoder[CreateUser]

}
