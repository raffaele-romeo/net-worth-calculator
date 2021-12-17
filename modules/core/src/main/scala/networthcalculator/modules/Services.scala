package networthcalculator.modules

import cats.Parallel
import cats.effect.{Concurrent, Resource, Timer}
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.services.{AssetsServiceImpl, HealthCheckServiceImpl}

object Services {

  def make[F[_]: Concurrent: Parallel: Timer](
      transactor: Resource[F, HikariTransactor[F]],
      redis: RedisCommands[F, String, String]
  ): Services[F] = {

    val assetsService = new AssetsServiceImpl[F](transactor)
    val healthCheckService = new HealthCheckServiceImpl[F](transactor, redis)
    Services[F](assetsService, healthCheckService)
  }

  final case class Services[F[_]](
      assetsService: AssetsServiceImpl[F],
      healthCheckService: HealthCheckServiceImpl[F]
  )
}
