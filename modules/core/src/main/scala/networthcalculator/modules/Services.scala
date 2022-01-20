package networthcalculator.modules

import cats.effect.{Resource, Async}
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.services.{HealthCheckServiceImpl, AssetsServiceImpl}
import networthcalculator.algebras.{AssetsService, HealthCheckService}
import org.typelevel.log4cats.Logger
import networthcalculator.algebras.TransactionsService
import networthcalculator.services.TransactionServiceImpl
import networthcalculator.programs.{TransactionProgram, TransactionProgramImpl}

object Services {
  def make[F[_]: Async](
      transactor: Resource[F, HikariTransactor[F]],
      redis: RedisCommands[F, String, String]
  ): Services[F] = {
    val healthCheckService = HealthCheckServiceImpl.make[F](transactor, redis)
    val assetService       = AssetsServiceImpl.make[F](transactor)
    val transactionService = TransactionServiceImpl.make[F](transactor)
    val transactionProgram = TransactionProgramImpl.make[F]

    Services[F](healthCheckService, assetService, transactionService, transactionProgram)
  }
}

final case class Services[F[_]](
    healthCheckService: HealthCheckService[F],
    assetService: AssetsService[F],
    transactionsService: TransactionsService[F],
    transactionProgram: TransactionProgram[F]
)
