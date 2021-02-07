package networthcalculator

import cats.effect.{ConcurrentEffect, ContextShift, Resource, _}
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import doobie._
import doobie.hikari.HikariTransactor
import io.chrisdavenport.log4cats.Logger
import networthcalculator.config.data._

object AppResources {

  def make[F[_]: ConcurrentEffect: ContextShift: Logger](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] = {

    def mkPostgreSqlResource(c: PostgreSQLConfig): Resource[F, HikariTransactor[F]] = {
      for {
        ce <- ExecutionContexts.fixedThreadPool[F](c.max.value)
        be <- Blocker[F]
        xa <- HikariTransactor.newHikariTransactor[F](
          "org.postgresql.Driver",
          s"jdbc:postgresql://${c.host.value}:${c.port.value}/${c.database.value}",
          c.user.value,
          "",
          ce,
          be
        )
      } yield xa
    }

    def mkRedisResource(c: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.uri.value.value)

    for {
      redis <- mkRedisResource(cfg.redis)
    } yield AppResources[F](mkPostgreSqlResource(cfg.postgreSQL), redis)
  }
}

final case class AppResources[F[_]](
    psql: Resource[F, HikariTransactor[F]],
    redis: RedisCommands[F, String, String]
)
