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
import cats.data.ValidatedNec
import cats.data.Validated.{Invalid, Valid}
import cats.effect.Sync
import networthcalculator.domain.errors.*

object AuthServiceImpl {
  def make[F[_]](
      usersService: UsersService[F],
      encryptionService: EncryptionService[F],
      tokensService: TokensService[F],
      expiresIn: TokenExpiration
  )(implicit S: Sync[F], ME: MonadThrow[F]): AuthService[F] =
    new AuthService[F] {

      override def newUser(validUser: ValidUser): F[JwtToken] = {
        for {
          salt              <- encryptionService.generateRandomSalt()
          encryptedPassword <- encryptionService.encrypt(validUser.password, salt)
          user <- usersService
            .create(
              CreateUserForInsert(
                name = validUser.username,
                password = encryptedPassword,
                salt = salt
              )
            )
          token <- tokensService.generateToken(user.name, expiresIn, JWSAlgorithm.HS256)
          _     <- tokensService.storeToken(user.name, token, expiresIn)
        } yield token
      }

      override def login(validUser: ValidUser): F[JwtToken] = {
        usersService
          .find(validUser.username)
          .flatMap {
            case None => UserNotFound(validUser.username).raiseError[F, JwtToken]
            case Some(user) =>
              Monad[F].ifM(
                encryptionService.checkPassword(user.password, validUser.password, user.salt)
              )(
                tokensService.findTokenBy(validUser.username).flatMap {
                  case Some(token) => token.pure[F]
                  case None =>
                    for {
                      token <- tokensService.generateToken(user.name, expiresIn, JWSAlgorithm.HS256)
                      _     <- tokensService.storeToken(user.name, token, expiresIn)
                    } yield token
                },
                InvalidPassword(user.name).raiseError[F, JwtToken]
              )
          }
      }

      def validate(username: String, password: String): F[ValidUser] = {
        FormValidatorNec.validateForm(username: String, password: String) match {
          case Valid(user) =>
            user.pure[F]
          case Invalid(e) =>
            ME.raiseError(DomainValidationErrors(e.toNonEmptyList.toList.map(_.errorMessage)))
        }
      }
    }
}

object FormValidatorNec {

  type ValidationResult[A] = ValidatedNec[DomainValidation, A]

  private def validateUserName(userName: String): ValidationResult[String] =
    if (
      userName.matches(
        "^(?=.{1,64}@)[\\p{L}0-9_-]+(\\.[\\p{L}0-9_-]+)*@[^-][\\p{L}0-9-]+(\\.[\\p{L}0-9-]+)*(\\.[\\p{L}]{2,})$"
      )
    ) userName.validNec
    else UsernameHasSpecialCharacters.invalidNec

  private def validatePassword(password: String): ValidationResult[String] =
    if (password.matches("(?=^.{10,}$)((?=.*\\d)|(?=.*\\W+))(?![.\\n])(?=.*[A-Z])(?=.*[a-z]).*$"))
      password.validNec
    else PasswordDoesNotMeetCriteria.invalidNec

  def validateForm(
      username: String,
      password: String
  ): ValidationResult[ValidUser] = {
    (
      validateUserName(username),
      validatePassword(password)
    ).mapN((username, password) => ValidUser(UserName(username), Password(password)))
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
