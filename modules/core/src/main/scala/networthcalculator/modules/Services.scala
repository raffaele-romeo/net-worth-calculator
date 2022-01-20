package networthcalculator.modules

import cats.effect.{Async, Resource}
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.services.{
  AssetsServiceImpl,
  HealthCheckServiceImpl,
  TransactionServiceImpl,
  ValidationServiceImpl
}
import networthcalculator.algebras.{
  AssetsService,
  HealthCheckService,
  TransactionsService,
  ValidationService
}
import org.typelevel.log4cats.Logger

object Services {
  def make[F[_]: Async](
      transactor: Resource[F, HikariTransactor[F]],
      redis: RedisCommands[F, String, String]
  ): Services[F] = {
    val healthCheckService = HealthCheckServiceImpl.make[F](transactor, redis)
    val assetService       = AssetsServiceImpl.make[F](transactor)
    val transactionService = TransactionServiceImpl.make[F](transactor)
    val validationService  = ValidationServiceImpl.make[F]

    Services[F](healthCheckService, assetService, transactionService, validationService)
  }
}

final case class Services[F[_]](
    healthCheckService: HealthCheckService[F],
    assetService: AssetsService[F],
    transactionsService: TransactionsService[F],
    validationService: ValidationService[F]
)
