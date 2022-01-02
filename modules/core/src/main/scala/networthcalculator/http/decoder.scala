package networthcalculator.http

import cats.syntax.all._
import networthcalculator.effects._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger

object decoder {

  implicit class RefinedRequestDecoder[F[_]: MonadThrow: Logger](req: Request[F])
      extends Http4sDsl[F] {

    def decodeR[A](
        f: A => F[Response[F]]
    )(implicit entityDecoder: EntityDecoder[F, A]): F[Response[F]] = {
      req.as[A].attempt.flatMap {
        case Left(e) =>
          Logger[F].error(s"Failed to decoder request: $e") >> {
            Option(e.getCause) match {
              case Some(c) if c.getMessage.startsWith("Predicate") => BadRequest(c.getMessage)
              case _                                               => UnprocessableEntity()
            }
          }
        case Right(a) => Logger[F].info(s"Decoder request successful $a") >> f(a)
      }
    }

  }

}
