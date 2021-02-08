package networthcalculator.config

import cats.effect._
import ciris._
import eu.timepit.refined.auto._
import networthcalculator.config.data._
import networthcalculator.config.environments.AppEnvironment._
import networthcalculator.config.environments._

import scala.concurrent.duration._

object Loader {

  def apply[F[_]: Async: ContextShift]: F[AppConfig] =
    env("SC_APP_ENV")
      .as[AppEnvironment]
      .map {
        case Test =>
          default(
            redisUri = RedisURI("redis://localhost")
          )
        case Prod =>
          default(
            redisUri = RedisURI("redis://prod-host")
          )
      }
      .load[F]

  private def default(redisUri: RedisURI): AppConfig =
    AppConfig(
      TokenExpiration(30.minutes),
      PostgreSQLConfig(
        host = "localhost",
        port = 5432,
        user = "postgres",
        database = "networth",
        max = 10
      ),
      RedisConfig(redisUri),
      HttpServerConfig(
        host = "0.0.0.0",
        port = 8080
      )
    )
}
