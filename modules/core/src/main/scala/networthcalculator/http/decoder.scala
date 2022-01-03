package networthcalculator.http

import cats.syntax.all._
import networthcalculator.effects._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger
import io.circe.DecodingFailure

object decoder {

  implicit class RefinedRequestDecoder[F[_]: MonadThrow: Logger](req: Request[F])
      extends Http4sDsl[F] {
    def decodeR[A](
        f: A => F[Response[F]]
    )(implicit entityDecoder: EntityDecoder[F, A]): F[Response[F]] = {
      req.as[A].attempt.flatMap {
        case Left(e) =>
          Logger[F].error(s"Failed to decoder request with error: $e") >> {
            e.getCause match {
              case d: DecodingFailure => BadRequest(d.show)
              case _                  => BadRequest(e.toString)
            }
          }
        case Right(a) => f(a)
      }
    }

  }

}
