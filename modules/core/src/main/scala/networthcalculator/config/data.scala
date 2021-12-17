package networthcalculator.config

import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.AdminUser

import scala.concurrent.duration._

object data {
  final case class TokenExpiration(value: FiniteDuration)

  final case class AppConfig(
      tokenExpiration: TokenExpiration,
      jwtAdmin: JWTAdmin,
      postgreSQL: PostgreSQLConfig,
      redis: RedisConfig,
      httpServerConfig: HttpServerConfig
  )

  final case class PostgreSQLConfig(
      host: String,
      port: Int,
      user: String,
      database: String,
      max: Int
  )

  final case class RedisURI(value: String)
  final case class RedisConfig(uri: RedisURI)

  final case class HttpServerConfig(
      host: String,
      port: Int
  )

  final case class JWTAdmin(
      adminToken: JwtToken,
      adminUser: AdminUser
  )
}
