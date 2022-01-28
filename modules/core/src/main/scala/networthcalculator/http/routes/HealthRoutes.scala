package networthcalculator.http.routes

import cats.Monad
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import networthcalculator.algebras.HealthCheckService
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final class HealthRoutes[F[_]: Monad](
  healthCheck: HealthCheckService[F]
) extends Http4sDsl[F]:

  private[routes] val prefixPath = "/healthcheck"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    for
      status   <- healthCheck.status
      response <- Ok(status.asJson)
    yield response
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
