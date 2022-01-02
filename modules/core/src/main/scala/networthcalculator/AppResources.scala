package networthcalculator

import cats.effect.kernel.Async
import cats.effect._
import dev.profunktor.redis4cats.effect.Log.Stdout._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import doobie._
import doobie.hikari.HikariTransactor
import networthcalculator.config.data._

object AppResources {
  def make[F[_]: Async](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] = {

    def mkPostgreSqlResource(c: PostgreSQLConfig): Resource[F, HikariTransactor[F]] = {
      for {
        ce <- ExecutionContexts.fixedThreadPool[F](c.max)
        xa <- HikariTransactor.newHikariTransactor[F](
          "org.postgresql.Driver",
          s"jdbc:postgresql://${c.host}:${c.port}/${c.database}",
          c.user,
          "",
          ce
        )
      } yield xa
    }

    def mkRedisResource(c: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.uri.value)

    for {
      redis <- mkRedisResource(cfg.redis)
    } yield AppResources[F](mkPostgreSqlResource(cfg.postgreSQL), redis)
  }
}

final case class AppResources[F[_]](
    psql: Resource[F, HikariTransactor[F]],
    redis: RedisCommands[F, String, String]
)
