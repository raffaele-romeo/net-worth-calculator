package networthcalculator.modules

import cats.effect.kernel.{MonadCancelThrow, Temporal}
import cats.effect.Resource
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.services.{AssetsServiceImpl, HealthCheckServiceImpl}
import networthcalculator.algebras.{AssetsService, HealthCheckService}

object Services {
  def make[F[_]: MonadCancelThrow: Temporal](
      transactor: Resource[F, HikariTransactor[F]],
      redis: RedisCommands[F, String, String]
  ): Services[F] = {
    val assetsService      = AssetsServiceImpl.make[F](transactor)
    val healthCheckService = HealthCheckServiceImpl.make[F](transactor, redis)

    Services[F](assetsService, healthCheckService)
  }
}

final case class Services[F[_]](
    assetsService: AssetsService[F],
    healthCheckService: HealthCheckService[F]
)
