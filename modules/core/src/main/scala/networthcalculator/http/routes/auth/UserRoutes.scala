package networthcalculator.http.routes.auth

import cats.Defer
import cats.syntax.all._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import networthcalculator.algebras.AuthService
import networthcalculator.domain.users._
import networthcalculator.effects.MonadThrow
import networthcalculator.http.decoder._
import networthcalculator.http.json._
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final class UserRoutes[F[_]: Defer: JsonDecoder: MonadThrow: SelfAwareStructuredLogger](
    authService: AuthService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      req
        .decodeR[CreateUser] { user =>
          authService
            .newUser(user.username.toDomain, user.password.toDomain)
            .flatMap(Created(_))
        }
        .recoverWith {
          case UserNameInUse(u) => Conflict(u.value)
        }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
