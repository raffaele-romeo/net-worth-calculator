package networthcalculator.services

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.effect.Sync
import cats.implicits.*
import cats.syntax.all.*
import cats.{Applicative, Monad, MonadThrow}
import com.nimbusds.jose.JWSAlgorithm
import networthcalculator.algebras._
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.errors.AuthValidation._
import networthcalculator.domain.errors.{AuthValidation, AuthValidationErrors}
import networthcalculator.domain.tokens.*
import networthcalculator.domain.users.*

object AuthServiceImpl {
  def make[F[_]](
      usersService: UsersService[F],
      encryptionService: EncryptionService[F],
      tokensService: TokensService[F],
      expiresIn: TokenExpiration
  )(using S: Sync[F], ME: MonadThrow[F]): AuthService[F] =
    new AuthService[F] {

      override def newUser(validUser: ValidUser): F[JwtToken] = {
        for {
          salt              <- encryptionService.generateRandomSalt()
          encryptedPassword <- encryptionService.encrypt(validUser.password, salt)
          user <- usersService
            .create(
              CreateUserForInsert(
                username = validUser.username,
                password = encryptedPassword,
                salt = salt
              )
            )
          token <- tokensService.generateToken(user.username, expiresIn, JWSAlgorithm.HS256)
          _ <- tokensService.storeToken(CommonUser(user.userId, user.username), token, expiresIn)
        } yield token
      }

      override def login(validUser: ValidUser): F[JwtToken] = {
        usersService
          .find(validUser.username)
          .flatMap {
            case None => UserNotFound(validUser.username).raiseError[F, JwtToken]
            case Some(user) =>
              ME.ifM(
                encryptionService.checkPassword(user.password, validUser.password, user.salt)
              )(
                tokensService.findTokenBy(validUser.username).flatMap {
                  case Some(token) => token.pure[F]
                  case None =>
                    for {
                      token <- tokensService
                        .generateToken(user.username, expiresIn, JWSAlgorithm.HS256)
                      _ <- tokensService
                        .storeToken(CommonUser(user.userId, user.username), token, expiresIn)
                    } yield token
                },
                InvalidPassword(user.username).raiseError[F, JwtToken]
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
      (token == adminToken)
        .guard[Option]
        .as(adminUser)
        .pure[F]

  def common[F[_]](
      tokensService: TokensService[F]
  ): UsersAuthService[F, CommonUser] =
    (token: JwtToken) =>
      tokensService
        .findUserBy(token)

}
