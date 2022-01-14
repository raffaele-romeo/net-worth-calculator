package networthcalculator.http.routes.auth

import cats.effect.Concurrent
import cats.implicits._
import networthcalculator.algebras.AuthService
import networthcalculator.domain.tokens.UserNotFound
import networthcalculator.domain.users._
import networthcalculator.http.decoder._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import networthcalculator.domain.errors.DomainValidationErrors
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder

final class LoginRoutes[F[_]: Concurrent: Logger](
    authService: AuthService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    req
      .decodeR[LoginUser] { user =>
        for {
          validUser <- authService.validate(user.username, user.password)
          result <- authService
            .login(validUser)
            .flatMap(jwtToken => Ok(jwtToken.toString.asJson))
        } yield result
      }
      .recoverWith {
        case DomainValidationErrors(errors) =>
          BadRequest(errors.asJson)
        case UserNotFound(_) | InvalidPassword(_) =>
          Forbidden()
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
