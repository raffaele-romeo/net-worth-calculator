package networthcalculator.http.routes.auth

import cats.effect.kernel.Concurrent
import cats.implicits._
import networthcalculator.algebras.AuthService
import networthcalculator.domain.tokens.UserNotFound
import networthcalculator.domain.users._
import networthcalculator.effects.MonadThrow
import networthcalculator.http.decoder._
import networthcalculator.http.json._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

final class LoginRoutes[F[_]: Concurrent: MonadThrow: Logger](
    authService: AuthService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req
        .decodeR[LoginUser] { user =>
          authService
            .login(user.username.toDomain, user.password.toDomain)
            .flatMap(Ok(_))
        }
        .recoverWith {
          case UserNotFound(_) | InvalidPassword(_) => Forbidden()
        }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
