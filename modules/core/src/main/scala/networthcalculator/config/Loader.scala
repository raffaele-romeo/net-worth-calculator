package networthcalculator.config

import cats.effect.Async
import cats.syntax.parallel.*
import ciris.*
import networthcalculator.config.data.*
import org.http4s.implicits.uri

import java.util.UUID
import scala.concurrent.duration.*

object Loader:

  def apply[F[_]: Async]: F[AppConfig] =
    (
      env("PORT").as[Int].default(9000),
      env("DB_HOST").as[String].default("localhost"),
      env("DB_USER").as[String].default("postgres"),
      env("DB_PASSWORD").as[String].default("secret").secret,
      env("DB_NAME").as[String].default("networth"),
      env("REDIS_URI").as[String].default("redis://localhost"),
      env("CURRENCY_API_KEY")
        .as[String]
        .default("ce51a410-7bbd-11ec-a2fe-b7a0c5b16d51")
        .secret,
      env("ALLOWED_ORIGINS")
        .as[String]
        .default("http://localhost:5173")
    ).parMapN { (port, dbHost, dbUser, dbPassword, dbName, redisUri, apiKey, allowedOrigins) =>
      AppConfig(
        TokenExpiration(60.minutes),
        PostgreSQLConfig(
          host = Host(dbHost),
          port = Port(5432),
          user = User(dbUser),
          password = Password(dbPassword.value),
          database = DatabaseName(dbName),
          max = MaxConnections(10)
        ),
        RedisConfig(RedisURI(redisUri)),
        HttpServerConfig(
          host = Host("0.0.0.0"),
          port = Port(port),
          allowedOrigins = allowedOrigins.split(",").map(_.trim).filter(_.nonEmpty).toSet
        ),
        CurrencyConversionConfig(
          baseUri = uri"https://freecurrencyapi.net/api/v2/latest",
          apiKey = UUID.fromString(apiKey.value)
        )
      )
    }.load[F]
