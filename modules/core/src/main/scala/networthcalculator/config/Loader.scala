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
            redisUri = RedisURI("redis://redis")
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
      JWTAdmin(
        JwtToken(
          // Make it a secret with env variable
          "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyYWZmYWVsZUBmYWtlLmNvbSIsIm5iZiI6MTY0MTIyODE0OCwiaXNzIjoibmV0LXdvcnRoLWNhbGN1bGF0b3IiLCJleHAiOjE2NDEyMjk5NDgsImlhdCI6MTY0MTIyODE0OCwianRpIjoiOGQ0Yzc3NWUtMzA2MS00YTNmLTllMGItNmE0YTZlNmI5OWE2In0.-MUWm3bFVhhxuz8l1WUnEqX0jmzAjcDC9T4mWXeaCfs"
        ),
        AdminUser(UserName("admin"))
      ),
      PostgreSQLConfig(
        host = Host("postgres"),
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
