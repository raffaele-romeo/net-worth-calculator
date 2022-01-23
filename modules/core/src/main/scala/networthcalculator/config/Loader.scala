package networthcalculator.config

import cats.effect._
import cats.implicits._
import ciris._
import networthcalculator.config.data._
import networthcalculator.config.environments.AppEnvironment._
import networthcalculator.config.environments._
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{AdminUser, UserName}
import org.http4s.Uri
import org.http4s.implicits.uri

import scala.concurrent.duration._
import java.util.UUID

object Loader {

  def apply[F[_]: Async]: F[AppConfig] =
    env("NWC_APP_ENV")
      .as[AppEnvironment]
      .default(Local)
      .map {
        case Local =>
          default(
            postgresHost = Host("localhost"),
            redisUri = RedisURI("redis://localhost")
          )
        case Test =>
          default(
            postgresHost = Host("postgres"),
            redisUri = RedisURI("redis://redis")
          )
      }
      .load[F]

  private def default(postgresHost: Host, redisUri: RedisURI): AppConfig =
    AppConfig(
      TokenExpiration(60.minutes),
      PostgreSQLConfig(
        host = postgresHost,
        port = Port(5432),
        user = User("postgres"),
        password = Password("secret"), // TODO This should be hidden
        database = DatabaseName("networth"),
        max = MaxConnections(10)
      ),
      RedisConfig(redisUri),
      HttpServerConfig(
        host = Host("0.0.0.0"),
        port = Port(9000)
      ),
      CurrencyConversionConfig(
        baseUri = uri"https://freecurrencyapi.net/api/v2/latest",
        apiKey = UUID.fromString(
          "ce51a410-7bbd-11ec-a2fe-b7a0c5b16d51"
        ) // TODO This should be hidden
      )
    )
}
