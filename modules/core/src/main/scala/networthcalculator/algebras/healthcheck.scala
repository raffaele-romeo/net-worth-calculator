package networthcalculator.algebras

import cats.Parallel
import cats.effect.{Concurrent, Resource, Sync, Timer}
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.domain.healthcheck.AppStatus
import tsec.authentication.AugmentedJWT
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256
import networthcalculator.domain.healthcheck._
import cats.effect.implicits._
import cats.syntax.all._
import doobie.ConnectionIO
import doobie.implicits._
import networthcalculator.domain.users.UserName

import scala.concurrent.duration._

trait HealthCheck[F[_]] {
  def status: F[AppStatus]
}

object LiveHealthCheck {

  def make[F[_]: Concurrent: Parallel: Timer](
      transactor: Resource[F, HikariTransactor[F]],
      redis: RedisCommands[F, SecureRandomId, AugmentedJWT[HMACSHA256, UserName]]
  ): F[HealthCheck[F]] =
    Sync[F].delay(
      new LiveHealthCheck[F](transactor, redis)
    )
}

final class LiveHealthCheck[F[_]: Concurrent: Parallel: Timer] private (
    transactor: Resource[F, HikariTransactor[F]],
    redis: RedisCommands[F, SecureRandomId, AugmentedJWT[HMACSHA256, UserName]]
) extends HealthCheck[F] {

  val q: ConnectionIO[Option[Int]] =
    sql"SELECT pid FROM pg_stat_activity".query[Int].option

  val redisHealth: F[RedisStatus] =
    redis.ping
      .map(_.nonEmpty)
      .timeout(1.second)
      .orElse(false.pure[F])
      .map(RedisStatus.apply)

  val postgresHealth: F[PostgresStatus] =
    transactor
      .use(q.transact[F])
      .map(_.nonEmpty)
      .timeout(1.second)
      .orElse(false.pure[F])
      .map(PostgresStatus.apply)

  val status: F[AppStatus] =
    (redisHealth, postgresHealth).parMapN(AppStatus)
}
