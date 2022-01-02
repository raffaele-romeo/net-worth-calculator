package networthcalculator.modules

import cats.effect.kernel.{Sync, Temporal}
import cats.effect.Resource
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.services.{AssetsServiceImpl, HealthCheckServiceImpl}

object Services {

  def make[F[_]: Sync: Temporal](
      transactor: Resource[F, HikariTransactor[F]],
      redis: RedisCommands[F, String, String]
  ): Services[F] = {

    val assetsService      = new AssetsServiceImpl[F](transactor)
    val healthCheckService = new HealthCheckServiceImpl[F](transactor, redis)
    Services[F](assetsService, healthCheckService)
  }

  final case class Services[F[_]](
      assetsService: AssetsServiceImpl[F],
      healthCheckService: HealthCheckServiceImpl[F]
  )
}
