package networthcalculator.modules

import cats.Parallel
import cats.effect.{Concurrent, Resource, Timer}
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.algebras._
import networthcalculator.services.{AssetsServiceImpl, EncryptionServiceImpl, HealthCheckServiceImpl, TokensServiceImpl, UsersService}

object Services {

  def make[F[_]: Concurrent: Parallel: Timer](
      transactor: Resource[F, HikariTransactor[F]],
      redis: RedisCommands[F, String, String]
  ): Services[F] = {
    val usersService = new UsersService[F](transactor)
    val tokensService = new TokensServiceImpl[F](redis)
    val encryptionService = new EncryptionServiceImpl
    val assetsService = new AssetsServiceImpl[F](transactor)
    val healthCheckService = new HealthCheckServiceImpl[F](transactor, redis)

    Services[F](usersService, tokensService, encryptionService, assetsService, healthCheckService)
  }

  final case class Services[F[_]](
                                   users: UsersService[F],
                                   tokens: TokensServiceImpl[F],
                                   encryption: EncryptionServiceImpl,
                                   assets: AssetsServiceImpl[F],
                                   healthCheck: HealthCheckServiceImpl[F]
  )
}
