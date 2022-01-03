package networthcalculator.http.routes

import cats.Monad
import networthcalculator.algebras.HealthCheckService
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import cats.implicits._

final class HealthRoutes[F[_]: Monad](
    healthCheck: HealthCheckService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/healthcheck"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    healthCheck.status.flatMap(status => Ok(status.asJson))
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
