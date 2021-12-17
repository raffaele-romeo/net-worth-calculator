package networthcalculator.modules

import cats.Id
import cats.effect._
import cats.syntax.all._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.users._
import networthcalculator.http.routes.admin.AssetRoutes
import networthcalculator.http.routes.auth._
import networthcalculator.http.routes.{HealthRoutes, version}
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._
import Services.Services
import scala.concurrent.duration._

final class HttpApi[F[_]: Concurrent: Timer: SelfAwareStructuredLogger] (
    services: Services[F],
    tokenExpiration: TokenExpiration
) {

  //Auth Routes
  private val loginRoutes =
    new LoginRoutes[F](services.users, services.encryption, services.tokens, tokenExpiration.value).routes
  private val logoutRoutes = new LogoutRoutes[F](jwtStatefulAuth).routes(auth)
  private val userRoutes = new UserRoutes[F](services.users, services.crypto, jwtStatefulAuth).routes

  // Open routes
  private val healthRoutes = new HealthRoutes[F](services.healthCheck).routes

  //Admin routes
  private val adminRoutes = new AssetRoutes[F](services.assets).routes(auth)

  private val nonAdminRoutes: HttpRoutes[F] =
    healthRoutes <+> userRoutes <+> loginRoutes <+> logoutRoutes

  private val routes: HttpRoutes[F] = Router(
    version.v1 -> nonAdminRoutes,
    version.v1 + "/admin" -> adminRoutes
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
