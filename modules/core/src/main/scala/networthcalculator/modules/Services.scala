package networthcalculator.modules

import cats.effect.Temporal
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.algebras._
import networthcalculator.services._
import org.typelevel.log4cats.Logger

object Services {
  def make[F[_]: Temporal](
      transactor: HikariTransactor[F],
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
