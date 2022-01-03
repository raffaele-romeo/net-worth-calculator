package networthcalculator.modules

import cats.effect._
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._

import networthcalculator.domain.users.{AdminUser, CommonUser}
import networthcalculator.http.routes.admin.AssetRoutes
import networthcalculator.http.routes.auth.{LoginRoutes, LogoutRoutes, UserRoutes}
import networthcalculator.http.routes.{HealthRoutes, version}
import networthcalculator.middleware.JWTAuthMiddleware
import networthcalculator.modules.Services

import scala.concurrent.duration._

object HttpApi {
  def make[F[_]: Async: Logger](
      services: Services[F],
      security: Security[F]
  ): HttpApp[F] = {

    val adminMiddleware =
      JWTAuthMiddleware[F, AdminUser](security.adminUsersAuthService.findUser)

    val usersMiddleware =
      JWTAuthMiddleware[F, CommonUser](security.commonUsersAuthService.findUser)

    // Auth Routes
    val loginRoutes =
      new LoginRoutes[F](security.authService).routes

    val logoutRoutes = new LogoutRoutes[F](security.tokensService).routes(usersMiddleware)
    val userRoutes   = new UserRoutes[F](security.authService).routes

    // Open routes
    val healthRoutes = new HealthRoutes[F](services.healthCheckService).routes

    // Admin routes
    val adminRoutes = new AssetRoutes[F](services.assetsService).routes(adminMiddleware)

    val nonAdminRoutes: HttpRoutes[F] =
      healthRoutes <+> userRoutes <+> loginRoutes <+> logoutRoutes

    val routes: HttpRoutes[F] = Router(
      version.v1            -> nonAdminRoutes,
      version.v1 + "/admin" -> adminRoutes
    )

    val middleware: HttpRoutes[F] => HttpRoutes[F] = {
      { (http: HttpRoutes[F]) =>
        AutoSlash(http)
      } andThen { (http: HttpRoutes[F]) =>
        Timeout(60.seconds)(http)
      }
    }

    val loggers: HttpApp[F] => HttpApp[F] = {
      { (http: HttpApp[F]) =>
        RequestLogger.httpApp(logHeaders = true, logBody = false)(http)
      } andThen { (http: HttpApp[F]) =>
        ResponseLogger.httpApp(logHeaders = true, logBody = false)(http)
      }
    }

    loggers(middleware(routes).orNotFound)

  }
}
