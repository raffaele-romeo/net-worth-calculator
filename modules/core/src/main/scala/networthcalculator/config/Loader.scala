package networthcalculator.config

import cats.effect._
import ciris._
import networthcalculator.config.data._
import networthcalculator.config.environments.AppEnvironment._
import networthcalculator.config.environments._
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{AdminUser, UserName}

import scala.concurrent.duration._

object Loader {

  def apply[F[_]: Async]: F[AppConfig] =
    env("NWC_APP_ENV")
      .as[AppEnvironment]
      .default(Test)
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
        host = Host("localhost"),
        port = Port(5432),
        user = User("postgres"),
        password = Password("secret"),
        database = DatabaseName("networth"),
        max = MaxConnections(10)
      ),
      RedisConfig(redisUri),
      HttpServerConfig(
        host = Host("0.0.0.0"),
        port = Port(8080)
      )
    )
}
