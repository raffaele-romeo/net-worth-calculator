package networthcalculator.http.routes.auth

import cats.Defer
import cats.syntax.all._
import networthcalculator.algebras.TokensService
import networthcalculator.effects.MonadThrow
import org.http4s.{AuthedRoutes, HttpRoutes}
import networthcalculator.domain.users._
import networthcalculator.middleware.AuthHeaders
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class LogoutRoutes[F[_]: MonadThrow: Defer](
    tokens: TokensService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: AuthedRoutes[UserName, F] = AuthedRoutes.of {

    case ar @ POST -> Root / "logout" as user =>
      AuthHeaders
        .getBearerToken(ar.req)
        .traverse_(tokens.deleteToken(user, _)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, UserName]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
