package networthcalculator.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import networthcalculator.domain.tokens.{InvalidJWTToken, JwtToken}
import networthcalculator.domain.users.{User, UserName}
import networthcalculator.effects.MonadThrow
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes, Request}
import org.http4s.server.AuthMiddleware
import org.http4s._
import org.http4s.Credentials.Token
import org.http4s.headers.Authorization
import cats.syntax.all._
import com.nimbusds.jwt.SignedJWT

import java.text.ParseException

object JWTAuthMiddleware {

  def apply[F[_]](
      authenticate: JwtToken => F[Option[UserName]]
  )(implicit S: Sync[F], ME: MonadThrow[F]): AuthMiddleware[F, UserName] = {

    val dsl = new Http4sDsl[F] {}; import dsl._

    val authUser: Kleisli[F, Request[F], Either[String, UserName]] = Kleisli(request =>
      AuthHeaders.getBearerToken(request).fold("Bearer token not found".asLeft[UserName].pure[F]) { token =>
        S.delay(SignedJWT.parse(token.value))
          .void
          .recoverWith {
            case _: ParseException => ME.raiseError(InvalidJWTToken)
          }
          .flatMap(_ => authenticate(token))
          .map(_.fold("not found".asLeft[String])(_.asRight[UserName]))
      }
    )

    val onFailure: AuthedRoutes[String, F] = Kleisli(req => OptionT.liftF(Forbidden(req.context)))

    AuthMiddleware(authUser, onFailure)
  }

}

object AuthHeaders {

  def getBearerToken[F[_]](request: Request[F]): Option[JwtToken] =
    request.headers.get(Authorization).collect {
      case Authorization(Token(AuthScheme.Bearer, token)) => JwtToken(token)
    }
}
