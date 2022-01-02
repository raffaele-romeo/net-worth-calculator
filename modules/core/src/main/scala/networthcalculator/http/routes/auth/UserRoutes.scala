package networthcalculator.http.routes.auth

import cats.effect.kernel.Concurrent
import cats.syntax.all._
import networthcalculator.algebras.AuthService
import networthcalculator.domain.users._
import networthcalculator.http.decoder._
import networthcalculator.http.json._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

final class UserRoutes[F[_]: Concurrent: Logger](
    authService: AuthService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "users" =>
    req
      .decodeR[CreateUser] { user =>
        authService
          .newUser(user.username.toDomain, user.password.toDomain)
          .flatMap(Created(_))
      }
      .recoverWith { case UserNameInUse(u) =>
        Conflict(u.value)
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
