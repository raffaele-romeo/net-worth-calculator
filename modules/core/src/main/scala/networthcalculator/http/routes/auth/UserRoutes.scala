package networthcalculator.http.routes.auth

import cats.effect.kernel.Concurrent
import cats.syntax.all._
import networthcalculator.algebras.AuthService
import networthcalculator.domain.users._
import networthcalculator.http.decoder._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import networthcalculator.domain.errors.DomainValidationErrors

final class UserRoutes[F[_]: Concurrent: Logger](
    authService: AuthService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath                               = "/auth"
  implicit val createUserDecoder: EntityDecoder[F, CreateUser] = jsonOf[F, CreateUser]

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "users" =>
    req
      .decodeR[CreateUser] { user =>
        for {
          validUser <- authService.validate(user.username, user.password)
          result <- authService
            .newUser(validUser)
            .flatMap(jwtToken => Created(jwtToken.asJson))
        } yield result
      }
      .recoverWith {
        case DomainValidationErrors(errors) =>
          BadRequest(errors.asJson)
        case UserNameInUse(u) =>
          Conflict(u.value)
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
