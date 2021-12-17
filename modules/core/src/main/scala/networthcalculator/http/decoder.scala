package networthcalculator.http

import cats.syntax.all._
import io.chrisdavenport.log4cats.{Logger, SelfAwareStructuredLogger}
import io.circe.Decoder
import networthcalculator.effects._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object decoder {

  implicit class RefinedRequestDecoder[F[_]: JsonDecoder: MonadThrow: SelfAwareStructuredLogger](req: Request[F])
      extends Http4sDsl[F] {

    def decodeR[A: Decoder](f: A => F[Response[F]]): F[Response[F]] = {
      Logger[F].info(s"Incoming request: $req") *>
        req.asJsonDecode[A].attempt.flatMap {
          case Left(e) =>
            Logger[F].error(s"Failed to decoder request: $e") >> {
              Option(e.getCause) match {
                case Some(c) if c.getMessage.startsWith("Predicate") => BadRequest(c.getMessage)
                case _ => UnprocessableEntity()
              }
            }
          case Right(a) => Logger[F].info(s"Decoder request successful $a") >> f(a)
        }
    }

  }

}
