package networthcalculator.http.routes.auth

import cats.syntax.all._
import networthcalculator.algebras.TokensService
import networthcalculator.domain.users._
import networthcalculator.effects.MonadThrow
import networthcalculator.middleware.AuthHeaders
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final class LogoutRoutes[F[_]: MonadThrow](
    tokens: TokensService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of { case ar @ POST -> Root / "logout" as user =>
    AuthHeaders
      .getBearerToken(ar.req)
      .traverse_(tokens.deleteToken(user.userName, _)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
