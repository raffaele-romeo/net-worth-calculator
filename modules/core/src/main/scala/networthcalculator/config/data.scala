package networthcalculator.config

import cats.Show
import ciris.*
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.AdminUser
import org.http4s.Uri

import java.util.UUID
import scala.annotation.targetName
import scala.concurrent.duration.*

object data:
  final case class AppConfig(
    tokenExpiration: TokenExpiration,
    postgreSQL: PostgreSQLConfig,
    redis: RedisConfig,
    httpServerConfig: HttpServerConfig,
    currencyConversionConfig: CurrencyConversionConfig
  )

  final case class PostgreSQLConfig(
    host: Host,
    port: Port,
    user: User,
    password: Password,
    database: DatabaseName,
    max: MaxConnections
  )

  final case class RedisConfig(uri: RedisURI)

  final case class CurrencyConversionConfig(baseUri: Uri, apiKey: UUID)

  final case class HttpServerConfig(
    host: Host,
    port: Port
  )

  opaque type TokenExpiration = FiniteDuration
  object TokenExpiration:
    def apply(d: FiniteDuration): TokenExpiration = d

  extension (x: TokenExpiration)
    @targetName("TokenExpiration")
    def toFiniteDuration: FiniteDuration = x

  opaque type RedisURI = String

  object RedisURI:
    def apply(d: String): RedisURI = d

  extension (x: RedisURI)
    @targetName("RedisURI")
    def toString: String = x

  opaque type Host = String
  object Host:
    def apply(d: String): Host = d

  extension (x: Host)
    @targetName("Host")
    def toString: String = x

  opaque type Port = Int
  object Port:
    def apply(d: Int): Port = d

  extension (x: Port)
    @targetName("Port")
    def toInt: Int = x

  opaque type User = String
  object User:
    def apply(d: String): User = d

  extension (x: User)
    @targetName("User")
    def toString: String = x

  opaque type Password = String

  object Password:
    def apply(d: String): Password = d

  extension (x: Password)
    @targetName("Password")
    def toString: String = x

  opaque type DatabaseName = String
  object DatabaseName:
    def apply(d: String): DatabaseName = d

  extension (x: DatabaseName)
    @targetName("DatabaseName")
    def toString: String = x

  opaque type MaxConnections = Int
  object MaxConnections:
    def apply(d: Int): MaxConnections = d

  extension (x: MaxConnections)
    @targetName("MaxConnections")
    def toInt: Int = x

  final case class JWTAdmin(
    adminToken: JwtToken,
    adminUser: AdminUser
  )
