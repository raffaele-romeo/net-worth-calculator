package networthcalculator

import cats.effect.{ Async, Resource }
import fs2.io.net.Network
import dev.profunktor.redis4cats.effect.Log.Stdout.*
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import doobie.*
import doobie.hikari.HikariTransactor
import networthcalculator.config.data.*
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

object AppResources:
  def make[F[_]: Async: Network](
    cfg: AppConfig
  ): Resource[F, AppResources[F]] =

    def mkPostgreSqlResource(
      c: PostgreSQLConfig
    ): Resource[F, HikariTransactor[F]] =
      for
        ce <- ExecutionContexts.fixedThreadPool[F](c.max.toInt)
        xa <- HikariTransactor.newHikariTransactor[F](
          "org.postgresql.Driver",
          s"jdbc:postgresql://${c.host.toString}:${c.port.toInt}/${c.database.toString}",
          c.user.toString,
          c.password.toString,
          ce
        )
      yield
        xa.kernel.setConnectionTimeout(2000)
        xa

    def mkRedisResource(
      c: RedisConfig
    ): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.uri.toString)

    for
      redis  <- mkRedisResource(cfg.redis)
      psql   <- mkPostgreSqlResource(cfg.postgreSQL)
      client <- EmberClientBuilder.default[F].build
    yield AppResources[F](psql, redis, client)

final case class AppResources[F[_]](
  psql: HikariTransactor[F],
  redis: RedisCommands[F, String, String],
  client: Client[F]
)
