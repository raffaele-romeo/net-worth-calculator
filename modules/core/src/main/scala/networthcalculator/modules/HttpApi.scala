package networthcalculator.modules

import cats.Id
import cats.effect._
import cats.syntax.all._
import networthcalculator.config.data.AppConfig
import networthcalculator.domain.users._
import networthcalculator.http.routes.auth._
import networthcalculator.http.routes.{HealthRoutes, version}
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._
import tsec.authentication.{AugmentedJWT, JWTAuthenticator, SecuredRequestHandler}
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import scala.concurrent.duration._

object HttpApi {

  def make[F[_]: Concurrent: Timer](algebras: Algebras[F], cfg: AppConfig): F[HttpApi[F]] = {
    Sync[F].delay(new HttpApi[F](algebras, cfg))
  }
}

final class HttpApi[F[_]: Concurrent: Timer] private (
    algebras: Algebras[F],
    cfg: AppConfig
) {

  private val signingKey: MacSigningKey[HMACSHA256] = HMACSHA256.generateKey[Id]

  private val jwtStatefulAuth: JWTAuthenticator[F, UserName, User, HMACSHA256] =
    JWTAuthenticator.backed.inBearerToken(
      expiryDuration = cfg.tokenExpiration.value,
      maxIdle = None,
      tokenStore = algebras.tokens,
      identityStore = algebras.users,
      signingKey = signingKey
    )

  private val Auth: SecuredRequestHandler[F, UserName, User, AugmentedJWT[HMACSHA256, UserName]] =
    SecuredRequestHandler(jwtStatefulAuth)

  //Auth Routes
  private val loginRoutes = new LoginRoutes[F](algebras.users, algebras.crypto, jwtStatefulAuth).routes
  private val logoutRoutes = new LogoutRoutes[F](jwtStatefulAuth).routes(Auth)
  private val userRoutes = new UserRoutes[F](algebras.users, algebras.crypto, jwtStatefulAuth).routes

  // Open routes
  private val healthRoutes = new HealthRoutes[F](algebras.healthCheck).routes

  //Admin routes
  //private val adminRoutes = new AssetRoutes[F]()

  private val nonAdminRoutes: HttpRoutes[F] =
    healthRoutes <+> userRoutes <+> loginRoutes <+> logoutRoutes

  private val routes: HttpRoutes[F] = Router(
    version.v1 -> nonAdminRoutes
    //version.v1 + "/admin" -> adminRoutes
  )

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    } andThen { http: HttpRoutes[F] =>
      CORS(http, CORS.DefaultCORSConfig)
    } andThen { http: HttpRoutes[F] =>
      Timeout(60.seconds)(http)
    }
  }

  private val loggers: HttpApp[F] => HttpApp[F] = {
    { http: HttpApp[F] =>
      RequestLogger.httpApp(true, true)(http)
    } andThen { http: HttpApp[F] =>
      ResponseLogger.httpApp(true, true)(http)
    }
  }

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)

}
