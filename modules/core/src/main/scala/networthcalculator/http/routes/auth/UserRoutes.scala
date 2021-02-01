package networthcalculator.http.routes.auth

import cats.Defer
import cats.syntax.all._
import networthcalculator.algebras.Crypto
import networthcalculator.effects.MonadThrow
import networthcalculator.http.decoder._
import networthcalculator.http.json._
import networthcalculator.domain.users._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s._
import tsec.authentication.{BackingStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256

final class UserRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    users: BackingStore[F, UserName, User],
    crypto: Crypto,
    auth: JWTAuthenticator[F, UserName, User, HMACSHA256]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      req
        .decodeR[CreateUser] { user =>
          val salt = crypto.generateRandomSalt(512)
          users
            .put(
              User(
                id = UserId(0), //value not used
                name = user.username.toDomain,
                password = crypto.encrypt(user.password.toDomain, salt),
                salt = salt
              )
            )
            .flatMap { user =>
              auth.create(user.name).map(auth.embed(Response(Status.Created), _))
            }
            .recoverWith {
              case UserNameInUse(u) => Conflict(u.value)
            }
        }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
