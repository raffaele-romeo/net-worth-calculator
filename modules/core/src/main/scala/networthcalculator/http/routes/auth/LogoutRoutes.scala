package networthcalculator.http.routes.auth

import cats.syntax.all._
import networthcalculator.domain.auth._
import networthcalculator.effects.MonadThrow
import org.http4s.HttpRoutes
import networthcalculator.domain.users._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import tsec.authentication._
import tsec.mac.jca.HMACSHA256

final class LogoutRoutes[F[_]: MonadThrow](
    auth: JWTAuthenticator[F, UserName, User, HMACSHA256]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: TSecAuthService[User, AugmentedJWT[HMACSHA256, UserName], F] =
    TSecAuthService.withAuthorization(CustomerRequired) {
      case request @ POST -> Root / "logout" asAuthed _ =>
        auth.discard(request.authenticator) *> NoContent()
    }

  def routes(
      secureRequestHandler: SecuredRequestHandler[F, UserName, User, AugmentedJWT[HMACSHA256, UserName]]
  ): HttpRoutes[F] =
    Router(
      prefixPath -> secureRequestHandler.liftService(httpRoutes)
    )
}
