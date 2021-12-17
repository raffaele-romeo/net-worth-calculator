package networthcalculator.http.routes.auth

import cats.Defer
import cats.syntax.all._
import com.nimbusds.jose.JWSAlgorithm
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import networthcalculator.algebras.{EncryptionService, TokensService, UsersService}
import networthcalculator.domain.users._
import networthcalculator.effects.MonadThrow
import networthcalculator.http.decoder._
import networthcalculator.http.json._
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import scala.concurrent.duration.FiniteDuration

final class UserRoutes[F[_]: Defer: JsonDecoder: MonadThrow: SelfAwareStructuredLogger](
                                                                                         users: UsersService[F],
                                                                                         encryption: EncryptionService,
                                                                                         tokens: TokensService[F],
                                                                                         expiresIn: FiniteDuration
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      req
        .decodeR[CreateUser] { user =>
          val salt = encryption.generateRandomSalt()
          for {
            user <- users
              .create(
                CreateUserForInsert(
                  name = user.username.toDomain,
                  password = encryption.encrypt(user.password.toDomain, salt),
                  salt = salt
                )
              )
            token <- tokens.generateToken(user.name, expiresIn, JWSAlgorithm.HS512)
            _ <- tokens.storeToken(user.name, token, expiresIn)
          } yield Created(token)
        }
        .recoverWith {
          case UserNameInUse(u) => Conflict(u.value)
        }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
