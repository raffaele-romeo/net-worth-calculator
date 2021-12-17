package networthcalculator.domain

import networthcalculator.domain.auth.Role

import scala.util.control.NoStackTrace

object users {

  final case class UserId(value: Long)

  final case class UserName(value: String)

  final case class Password(value: String)

  // --------- user registration -----------

  final case class Salt(value: String)

  final case class EncryptedPassword(value: String)

  final case class UserNameParam(value: String) {
    def toDomain: UserName = UserName(value.toLowerCase())
  }

  final case class PasswordParam(value: String) {
    def toDomain: Password = Password(value)
  }

  final case class CreateUser(username: UserNameParam, password: PasswordParam)

  final case class LoginUser(
      username: UserNameParam,
      password: PasswordParam
  )

  final case class CreateUserForInsert(name: UserName, password: EncryptedPassword, salt: Salt, role: Role = Role.User)

  final case class UserWithPassword(
      id: UserId,
      name: UserName,
      password: EncryptedPassword,
      salt: Salt,
      role: Role = Role.User
  )

  final case class AdminUser(userName: UserName)
  final case class CommonUser(userName: UserName)

  final case class UserNameInUse(username: UserName) extends NoStackTrace
  final case class InvalidPassword(username: UserName) extends NoStackTrace
}
