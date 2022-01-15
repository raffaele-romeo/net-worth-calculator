package networthcalculator.http

import cats.syntax.all._
import cats.MonadThrow
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger
import io.circe.DecodingFailure

object decoder {

  extension [F[_]: MonadThrow: Logger](req: Request[F]) {
    def decodeR[A](
        f: A => F[Response[F]]
    )(using entityDecoder: EntityDecoder[F, A]): F[Response[F]] = {
      val dsl = Http4sDsl[F]; import dsl._
      req.as[A].attempt.flatMap {
        case Left(e) =>
          Logger[F].error(s"Failed to decoder request with error: $e") >> {
            e.getCause match {
              case d: DecodingFailure => BadRequest(d.show)
              case _                  => UnprocessableEntity(e.toString)
            }
          }
        case Right(a) => f(a)
      }
    }

  }

}
