package networthcalculator.modules

import cats.effect.Async
import cats.syntax.all.*
import networthcalculator.domain.users.CommonUser

import networthcalculator.http.routes.auth.{
  LoginRoutes,
  LogoutRoutes,
  UserRoutes
}
import networthcalculator.http.routes.secured.{ AssetRoutes, TransactionRoutes }
import networthcalculator.http.routes.{ HealthRoutes, version }
import networthcalculator.middleware.JWTAuthMiddleware
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.*
import org.typelevel.ci.*
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.*

object HttpApi:
  def make[F[_]: Async: Logger](
    services: Services[F],
    security: Security[F],
    programs: Programs[F],
    allowedOrigins: Set[String]
  ): HttpApp[F] =

    val usersMiddleware =
      JWTAuthMiddleware[F, CommonUser](security.commonUsersAuthService.findUser)

    // Auth Routes
    val loginRoutes =
      new LoginRoutes[F](
        security.authService,
        services.validationService
      ).routes

    val logoutRoutes =
      new LogoutRoutes[F](security.tokensService).routes(usersMiddleware)
    val userRoutes =
      new UserRoutes[F](security.authService, services.validationService).routes

    // Open routes
    val healthRoutes = new HealthRoutes[F](services.healthCheckService).routes

    // Secured routes
    val assetsRoutes =
      new AssetRoutes[F](services.assetService, services.validationService)
        .routes(usersMiddleware)
    val transactionRoutes =
      new TransactionRoutes[F](
        services.transactionsService,
        services.validationService,
        programs.currencyExchangeRate
      )
        .routes(usersMiddleware)

    val nonAdminRoutes: HttpRoutes[F] =
      healthRoutes <+> userRoutes <+> loginRoutes <+> logoutRoutes <+> assetsRoutes <+> transactionRoutes

    val routes: HttpRoutes[F] = Router(
      version.v1 -> nonAdminRoutes
    )

    // Compare an incoming Origin host (e.g. "localhost:5173") against the
    // hostnames extracted from our allowed-origin URLs.
    val allowedHosts: Set[CIString] = allowedOrigins.flatMap { o =>
      Uri.fromString(o).toOption.flatMap(_.host).map(h => CIString(h.value))
    }

    val corsPolicy = CORS.policy
      .withAllowOriginHostCi(allowedHosts.contains)
      .withAllowMethodsIn(
        Set(Method.GET, Method.POST, Method.PUT, Method.DELETE, Method.OPTIONS)
      )
      .withAllowHeadersIn(Set(ci"Content-Type", ci"Authorization"))
      .withAllowCredentials(false)
      .withMaxAge(1.day)

    val middleware: HttpRoutes[F] => HttpRoutes[F] = { (http: HttpRoutes[F]) =>
      AutoSlash(http)
    } andThen { (http: HttpRoutes[F]) =>
      Timeout(60.seconds)(http)
    }

    val loggers: HttpApp[F] => HttpApp[F] = { (http: HttpApp[F]) =>
      RequestLogger.httpApp(logHeaders = true, logBody = false)(http)
    } andThen { (http: HttpApp[F]) =>
      ResponseLogger.httpApp(logHeaders = true, logBody = false)(http)
    }

    loggers(corsPolicy(middleware(routes).orNotFound))
