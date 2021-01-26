package networthcalculator.http.routes

import cats._
import cats.syntax.all._
import networthcalculator.algebras.Users
import networthcalculator.domain.auth._
import networthcalculator.effects.MonadThrow
import networthcalculator.http.decoder._
import networthcalculator.http.json._
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import tsec.authentication.{AugmentedJWT, SecuredRequestHandler, TSecAuthService}
import tsec.mac.jca.HMACSHA256
import org.http4s.server.Router

final class UserRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    users: Users[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: TSecAuthService[User, AugmentedJWT[HMACSHA256, Int], F] =
    TSecAuthService.withAuthorization(CustomerRequired) {
      case req @ POST -> Root / "users" asAuthed user =>
        req.request
          .decodeR[CreateUser] { user =>
            users
              .create(user.username.toDomain, user.password.toDomain)
              .flatMap(Created(_))
              .recoverWith {
                case UserNameInUse(u) => Conflict(u.value)
              }
          }
    }
  /*
   private val adminRequiredService: TSecAuthService[User, AugmentedJWT[HMACSHA256, Int], IO] =
    TSecAuthService.withAuthorization(AdminRequired) {
      case request @ GET -> Root / "api" / "admin-area" asAuthed user =>
        val r: SecuredRequest[IO, User, AugmentedJWT[HMACSHA256, Int]] = request
        Ok()
    }
   */

  def routes(secureRequestHandler: SecuredRequestHandler[F, Int, User, AugmentedJWT[HMACSHA256, Int]]): HttpRoutes[F] =
    Router(
      prefixPath -> secureRequestHandler.liftService(httpRoutes)
    )
}
