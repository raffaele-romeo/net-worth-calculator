package networthcalculator.config

import cats.effect._
import ciris._
import ciris.circe._
import cats.implicits._
import networthcalculator.config.data._
import networthcalculator.config.environments.AppEnvironment._
import networthcalculator.config.environments._
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{AdminUser, UserName}

import scala.concurrent.duration._

object Loader {

  def apply[F[_]: Async]: F[AppConfig] =
    (
      postgresConfig,
      redisConfig
    ).parMapN { (postgresConfig, redisConfig) =>
      AppConfig(
        TokenExpiration(30.minutes),
        postgresConfig,
        redisConfig,
        HttpServerConfig(
          host = Host("0.0.0.0"),
          port = Port(8080)
        )
      )
    }.load[F]

  val postgresConfig: ConfigValue[Effect, PostgreSQLConfig] =
    (
      env("DATABASE_HOST").as[Host],
      env("DATABASE_USERNAME").as[User],
      env("DATABASE_PASSWORD").as[Password].secret
    ).parMapN { (host, user, password) =>
      PostgreSQLConfig(
        host = host,
        port = Port(5432),
        user = user,
        password = password,
        database = DatabaseName("networth"),
        max = MaxConnections(10)
      )
    }

  val redisConfig: ConfigValue[Effect, RedisConfig] = {
    env("REDIS_HOST").as[RedisURI].map(RedisConfig.apply)
  }
}
