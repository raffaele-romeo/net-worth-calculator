package networthcalculator.modules

import cats.effect._
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import org.http4s._
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware._

import networthcalculator.domain.users.{AdminUser, CommonUser}
import networthcalculator.http.routes.account.AccountRoutes
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

    val usersMiddleware =
      JWTAuthMiddleware[F, CommonUser](security.commonUsersAuthService.findUser)

    // Auth Routes
    val loginRoutes =
      new LoginRoutes[F](security.authService).routes

    val logoutRoutes = new LogoutRoutes[F](security.tokensService).routes(usersMiddleware)
    val userRoutes   = new UserRoutes[F](security.authService).routes

    // Open routes
    val healthRoutes = new HealthRoutes[F](services.healthCheckService).routes

    // Account routes
    val accountsRoutes = new AccountRoutes[F](services.accountService).routes(usersMiddleware)

    val nonAdminRoutes: HttpRoutes[F] =
      healthRoutes <+> userRoutes <+> loginRoutes <+> logoutRoutes <+> accountsRoutes

    val routes: HttpRoutes[F] = Router(
      version.v1 -> nonAdminRoutes
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
