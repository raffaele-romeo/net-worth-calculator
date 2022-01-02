package networthcalculator.modules

import cats.effect.{Resource, Sync}
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.algebras.{AuthService, EncryptionService, TokensService, UsersAuthService, UsersService}
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{AdminUser, CommonUser}
import networthcalculator.services.{
  AuthServiceImpl,
  EncryptionServiceImpl,
  TokensServiceImpl,
  UsersAuthServiceImpl,
  UsersServiceImpl
}

object Security {
  def make[F[_]: Sync](
      transactor: Resource[F, HikariTransactor[F]],
      redis: RedisCommands[F, String, String],
      tokenExpiration: TokenExpiration,
      adminToken: JwtToken,
      adminUser: AdminUser
  ): Security[F] = {
    val usersService      = UsersServiceImpl.make[F](transactor)
    val tokensService     = TokensServiceImpl.make[F](redis)
    val encryptionService = EncryptionServiceImpl.make[F]
    val authService =
      AuthServiceImpl.make[F](usersService, encryptionService, tokensService, tokenExpiration)
    val adminUsersAuthService  = UsersAuthServiceImpl.admin(adminToken, adminUser)
    val commonUsersAuthService = UsersAuthServiceImpl.common(tokensService)

    Security(
      usersService,
      authService,
      encryptionService,
      tokensService,
      adminUsersAuthService,
      commonUsersAuthService
    )
  }

}

final case class Security[F[_]](
    usersService: UsersService[F],
    authService: AuthService[F],
    encryptionService: EncryptionService[F],
    tokensService: TokensService[F],
    adminUsersAuthService: UsersAuthService[F, AdminUser],
    commonUsersAuthService: UsersAuthService[F, CommonUser]
)
