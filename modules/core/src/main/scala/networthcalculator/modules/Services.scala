package networthcalculator.modules

import cats.effect.{Sync, Resource, Temporal}
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.services.{HealthCheckServiceImpl, AssetsServiceImpl}
import networthcalculator.algebras.{AssetsService, HealthCheckService}
import org.typelevel.log4cats.Logger

object Services {
  def make[F[_]: Sync: Temporal: Logger](
      transactor: Resource[F, HikariTransactor[F]],
      redis: RedisCommands[F, String, String]
  ): Services[F] = {
    val assetService       = AssetsServiceImpl.make[F](transactor)
    val healthCheckService = HealthCheckServiceImpl.make[F](transactor, redis)

    Services[F](healthCheckService, assetService)
  }
}

final case class Services[F[_]](
    healthCheckService: HealthCheckService[F],
    assetService: AssetsService[F]
)
