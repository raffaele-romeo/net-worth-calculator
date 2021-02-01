package networthcalculator.domain

import cats.MonadError
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import networthcalculator.domain.auth.Role
import tsec.authorization.AuthorizationInfo

import scala.util.control.NoStackTrace

object users {

  @newtype case class UserId(value: Long)

  @newtype case class UserName(value: String)

  @newtype case class Password(value: String)

  // --------- user registration -----------

  @newtype case class Salt(value: String)

  @newtype case class EncryptedPassword(value: String)

  @newtype case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = UserName(value.value.toLowerCase())
  }

  @newtype case class PasswordParam(value: NonEmptyString) {
    def toDomain: Password = Password(value.value)
  }

  case class CreateUser(username: UserNameParam, password: PasswordParam)

  case class LoginUser(
      username: UserNameParam,
      password: PasswordParam
  )

  case class User(id: UserId, name: UserName, password: EncryptedPassword, salt: Salt, role: Role = Role.Customer)

  object User {

    implicit def authRole[F[_]](implicit F: MonadError[F, Throwable]): AuthorizationInfo[F, Role, User] =
      (u: User) => F.pure(u.role)
  }

  case class UserNameInUse(username: UserName) extends NoStackTrace
  case class InvalidUserOrPassword(username: UserName) extends NoStackTrace

}
