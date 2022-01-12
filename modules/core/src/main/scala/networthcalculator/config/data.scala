package networthcalculator.config

import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.AdminUser

import scala.concurrent.duration._
import scala.annotation.targetName
import ciris._
import cats.Show

object data {
  final case class AppConfig(
      tokenExpiration: TokenExpiration,
      postgreSQL: PostgreSQLConfig,
      redis: RedisConfig,
      httpServerConfig: HttpServerConfig
  )

  final case class PostgreSQLConfig(
      host: Host,
      port: Port,
      user: User,
      password: Secret[Password],
      database: DatabaseName,
      max: MaxConnections
  )

  final case class RedisConfig(uri: RedisURI)

  final case class HttpServerConfig(
      host: Host,
      port: Port
  )

  opaque type TokenExpiration = FiniteDuration
  object TokenExpiration {
    def apply(d: FiniteDuration): TokenExpiration = d
  }

  extension (x: TokenExpiration) {
    @targetName("TokenExpiration")
    def toFiniteDuration: FiniteDuration = x
  }

  opaque type RedisURI = String

  object RedisURI {
    def apply(d: String): RedisURI = d

    given ConfigDecoder[String, RedisURI] = ConfigDecoder[String, String].map(apply)
  }

  extension (x: RedisURI) {
    @targetName("RedisURI")
    def toString: String = x
  }

  opaque type Host = String
  object Host {
    def apply(d: String): Host = d

    given ConfigDecoder[String, Host] = ConfigDecoder[String, String].map(apply)
  }

  extension (x: Host) {
    @targetName("Host")
    def toString: String = x
  }

  opaque type Port = Int
  object Port {
    def apply(d: Int): Port = d
  }

  extension (x: Port) {
    @targetName("Port")
    def toInt: Int = x
  }

  opaque type User = String
  object User {
    def apply(d: String): User = d

    given ConfigDecoder[String, User] = ConfigDecoder[String, String].map(apply)
  }

  extension (x: User) {
    @targetName("User")
    def toString: String = x
  }

  opaque type Password = String

  object Password {
    def apply(d: String): Password = d

    given ConfigDecoder[String, Password] = ConfigDecoder[String, String].map(apply)
    given showPassword: Show[Password]    = Show.show(_.toString)
  }

  extension (x: Password) {
    @targetName("Password")
    def toString: String = x
  }

  opaque type DatabaseName = String
  object DatabaseName {
    def apply(d: String): DatabaseName = d
  }

  extension (x: DatabaseName) {
    @targetName("DatabaseName")
    def toString: String = x
  }

  opaque type MaxConnections = Int
  object MaxConnections {
    def apply(d: Int): MaxConnections = d
  }

  extension (x: MaxConnections) {
    @targetName("MaxConnections")
    def toInt: Int = x
  }

  final case class JWTAdmin(
      adminToken: JwtToken,
      adminUser: AdminUser
  )
}
