package networthcalculator.http.routes.auth

import cats.effect.Concurrent
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import networthcalculator.algebras.{ AuthService, ValidationService }
import networthcalculator.domain.errors.AuthValidationErrors
import networthcalculator.domain.tokens.UserNotFound
import networthcalculator.domain.users.*
import networthcalculator.http.decoder.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

final class LoginRoutes[F[_]: Concurrent: Logger](
  authService: AuthService[F],
  validationService: ValidationService[F]
) extends Http4sDsl[F]:
  import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req
        .decodeR[LoginUser] { user =>
          for
            validUser <- validationService.validate(
              user.username,
              user.password
            )
            jwtToken <- authService
              .login(validUser)
            response <- Ok(jwtToken.toString.asJson)
          yield response
        }
        .recoverWith {
          case AuthValidationErrors(errors) =>
            BadRequest(errors.asJson)
          case UserNotFound(_) | InvalidPassword(_) =>
            Forbidden()
        }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
