package networthcalculator.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.all._
import com.nimbusds.jwt.SignedJWT
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.effects.MonadThrow
import org.http4s.Credentials.Token
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s._

import java.text.ParseException
import cats.instances.either
import cats.Show

object JWTAuthMiddleware {

  def apply[F[_], A: Show](
      authenticate: JwtToken => F[Option[A]]
  )(using S: Sync[F], ME: MonadThrow[F]): AuthMiddleware[F, A] = {

    val dsl = new Http4sDsl[F] {}
    import dsl._

    val authUser: Kleisli[F, Request[F], Either[String, A]] = Kleisli { request =>
      AuthHeaders.getBearerToken(request).fold("Bearer token not found".asLeft[A].pure[F]) {
        token =>
          S.delay(SignedJWT.parse(token.toString))
            .void
            .attempt
            .flatMap {
              _.fold(_ => none[A].pure[F], _ => authenticate(token))
            }
            .map {
              _.fold("Bearer token not found or invalid".asLeft[A])(_.asRight[String])
            }
      }
    }

    val onFailure: AuthedRoutes[String, F] = Kleisli(req => OptionT.liftF(Forbidden(req.context)))

    AuthMiddleware(authUser, onFailure)
  }

}

object AuthHeaders {

  def getBearerToken[F[_]](request: Request[F]): Option[JwtToken] =
    request.headers.get[Authorization].collect {
      case Authorization(Token(AuthScheme.Bearer, token)) =>
        JwtToken(token)
    }
}
