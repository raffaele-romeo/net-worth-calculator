package networthcalculator.services

import cats.{Applicative, Defer, Functor}
import cats.syntax.all._
import com.nimbusds.jose.JWSAlgorithm
import networthcalculator.algebras.{AuthService, EncryptionService, TokensService, UsersService, UsersAuthService}
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.users.CreateUserForInsert
import networthcalculator.domain.tokens._
import networthcalculator.domain.users._
import networthcalculator.effects.MonadThrow

final class AuthServiceImpl[F[_]: Defer: MonadThrow](
    usersService: UsersService[F],
    encryptionService: EncryptionService,
    tokensService: TokensService[F],
    expiresIn: TokenExpiration
) extends AuthService[F] {

  override def newUser(username: UserName, password: Password): F[JwtToken] = {
    val salt = encryptionService.generateRandomSalt()
    for {
      user <- usersService
        .create(
          CreateUserForInsert(
            name = username,
            password = encryptionService.encrypt(password, salt),
            salt = salt
          )
        )
      token <- tokensService.generateToken(user.name, expiresIn, JWSAlgorithm.HS512)
      _ <- tokensService.storeToken(user.name, token, expiresIn)
    } yield token
  }

  override def login(username: UserName, password: Password): F[JwtToken] = {
    usersService
      .find(username)
      .flatMap {
        case None => UserNotFound(username).raiseError[F, JwtToken]
        case Some(user) if !encryptionService.checkPassword(user.password, password, user.salt) =>
          InvalidPassword(user.name).raiseError[F, JwtToken]
        case Some(user) =>
          tokensService.findTokenBy(username).flatMap {
            case Some(token) => token.pure[F]
            case None =>
              for {
                token <- tokensService.generateToken(user.name, expiresIn, JWSAlgorithm.HS512)
                _ <- tokensService.storeToken(user.name, token, expiresIn)
              } yield token
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

  def common[F[_]: Functor](
      tokensService: TokensService[F]
  ): UsersAuthService[F, CommonUser] =
    (token: JwtToken) =>
      tokensService
        .findUserNameBy(token)

}
