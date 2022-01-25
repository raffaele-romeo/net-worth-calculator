package networthcalculator.services

import cats.effect.Temporal
import cats.effect.implicits._
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import networthcalculator.algebras.HealthCheckService
import networthcalculator.domain.healthcheck.{AppStatus, PostgresStatus, RedisStatus}

import scala.concurrent.duration._

object HealthCheckServiceImpl {
  def make[F[_]: Temporal](
      transactor: HikariTransactor[F],
      redis: RedisCommands[F, String, String]
  ): HealthCheckService[F] = new HealthCheckService[F] {

    val q: ConnectionIO[Option[Int]] =
      sql"SELECT pid FROM pg_stat_activity LIMIT 1".query[Int].option

    val redisHealth: F[RedisStatus] =
      redis.ping
        .map(_.nonEmpty)
        .timeout(1.second)
        .orElse(false.pure[F])
        .map(RedisStatus.apply)

    val postgresHealth: F[PostgresStatus] =
      q.transact[F](transactor)
        .map(_.nonEmpty)
        .timeout(1.second)
        .orElse(false.pure[F])
        .map(PostgresStatus.apply)

    val status: F[AppStatus] =
      (redisHealth, postgresHealth).parMapN(AppStatus.apply)
  }
}
