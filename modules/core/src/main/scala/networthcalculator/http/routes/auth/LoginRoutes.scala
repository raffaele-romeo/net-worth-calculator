package networthcalculator.http.routes.auth

import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import networthcalculator.algebras.{Crypto, Users}
import networthcalculator.domain.users._
import networthcalculator.http.decoder._
import networthcalculator.http.json._
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256

final class LoginRoutes[F[_]: JsonDecoder: Sync: SelfAwareStructuredLogger](
    users: IdentityStore[F, UserName, User] with Users[F],
    crypto: Crypto,
    auth: JWTAuthenticator[F, UserName, User, HMACSHA256]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req
        .decodeR[LoginUser] { user =>
          users
            .get(user.username.toDomain)
            .value
            .flatMap {
              case Some(userFound)
                  if crypto.checkPassword(userFound.password, user.password.toDomain, userFound.salt) =>
                auth.create(userFound.name).map(auth.embed(Response(Status.Ok), _))
              case _ => Sync[F].pure(Response(Status.Forbidden))
            }
        }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
