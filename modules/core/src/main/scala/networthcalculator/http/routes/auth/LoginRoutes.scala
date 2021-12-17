package networthcalculator.http.routes.auth

import cats.effect.Sync
import cats.implicits._
import com.nimbusds.jose.JWSAlgorithm
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import networthcalculator.algebras.{EncryptionService, TokensService, UsersService}
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.users._
import networthcalculator.http.decoder._
import networthcalculator.http.json._
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final class LoginRoutes[F[_]: JsonDecoder: SelfAwareStructuredLogger](
    users: UsersService[F],
    encryption: EncryptionService,
    tokens: TokensService[F],
    expiresIn: TokenExpiration
)(implicit S: Sync[F])
    extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req
        .decodeR[LoginUser] { user =>
          users
            .find(user.username.toDomain)
            .value
            .flatMap {
              case Some(userFound)
                  if encryption.checkPassword(userFound.password, user.password.toDomain, userFound.salt) =>
                tokens.findTokenBy(user.username.toDomain).flatMap {
                  case Some(token) => Ok(token.pure[F])
                  case None =>
                    for {
                      token <- tokens.generateToken(userFound.name, expiresIn, JWSAlgorithm.HS512)
                      _ <- tokens.storeToken(userFound.name, token, expiresIn)
                    } yield Ok(token)
                }
              case _ => Sync[F].pure(Response(Status.Forbidden))
            }
        }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
