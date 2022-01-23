package networthcalculator.modules

import cats.effect.{Resource, Sync}
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.algebras._
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{AdminUser, CommonUser}
import networthcalculator.services._

object Security {
  def make[F[_]: Sync](
      transactor: HikariTransactor[F],
      redis: RedisCommands[F, String, String],
      tokenExpiration: TokenExpiration
  ): Security[F] = {
    val usersService      = UsersServiceImpl.make[F](transactor)
    val tokensService     = TokensServiceImpl.make[F](redis)
    val encryptionService = EncryptionServiceImpl.make[F]
    val authService =
      AuthServiceImpl.make[F](usersService, encryptionService, tokensService, tokenExpiration)
    val commonUsersAuthService = UsersAuthServiceImpl.common(tokensService)

    Security(
      usersService,
      authService,
      encryptionService,
      tokensService,
      commonUsersAuthService
    )
  }

}

final case class Security[F[_]](
    usersService: UsersService[F],
    authService: AuthService[F],
    encryptionService: EncryptionService[F],
    tokensService: TokensService[F],
    commonUsersAuthService: UsersAuthService[F, CommonUser]
)
