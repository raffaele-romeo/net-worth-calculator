package networthcalculator.services

import cats.Applicative
import cats.syntax.all.*
import com.nimbusds.jose.JWSAlgorithm
import networthcalculator.algebras.{
  AuthService,
  EncryptionService,
  TokensService,
  UsersAuthService,
  UsersService
}
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.users.CreateUserForInsert
import networthcalculator.domain.tokens.*
import networthcalculator.domain.users.*
import networthcalculator.effects.MonadThrow
import cats.implicits.*
import cats.Monad
import cats.effect.Sync

object AuthServiceImpl {
  def make[F[_]: MonadThrow](
      usersService: UsersService[F],
      encryptionService: EncryptionService[F],
      tokensService: TokensService[F],
      expiresIn: TokenExpiration
  )(implicit S: Sync[F]): AuthService[F] =
    new AuthService[F] {

      override def newUser(username: UserName, password: Password): F[JwtToken] = {
        for {
          salt              <- encryptionService.generateRandomSalt()
          encryptedPassword <- encryptionService.encrypt(password, salt)
          user <- usersService
            .create(
              CreateUserForInsert(
                name = username,
                password = encryptedPassword,
                salt = salt
              )
            )
          token <- tokensService.generateToken(user.name, expiresIn, JWSAlgorithm.HS256)
          _     <- tokensService.storeToken(user.name, token, expiresIn)
        } yield token
      }

      override def login(username: UserName, password: Password): F[JwtToken] = {
        usersService
          .find(username)
          .flatMap {
            case None => UserNotFound(username).raiseError[F, JwtToken]
            case Some(user) =>
              Monad[F].ifM(encryptionService.checkPassword(user.password, password, user.salt))(
                tokensService.findTokenBy(username).flatMap {
                  case Some(token) => token.pure[F]
                  case None =>
                    for {
                      token <- tokensService.generateToken(user.name, expiresIn, JWSAlgorithm.HS512)
                      _     <- tokensService.storeToken(user.name, token, expiresIn)
                    } yield token
                },
                InvalidPassword(user.name).raiseError[F, JwtToken]
              )
          }
      }
    }
}

object UsersAuthServiceImpl {

  def admin[F[_]: Applicative](
      adminToken: JwtToken,
      adminUser: AdminUser
  ): UsersAuthService[F, AdminUser] =
    (token: JwtToken) =>
      (token.value == adminToken.value)
        .guard[Option]
        .as(adminUser)
        .pure[F]

  def common[F[_]](
      tokensService: TokensService[F]
  ): UsersAuthService[F, CommonUser] =
    (token: JwtToken) =>
      tokensService
        .findUserNameBy(token)

}
