package networthcalculator.domain

import doobie.util.{Get, Put, Read, Write}
import networthcalculator.domain.auth.Role

import scala.util.control.NoStackTrace

object users {

  final case class UserId(value: Long) extends AnyVal

  final case class UserName private (value: String) extends AnyVal

  final case class Password(value: String) extends AnyVal

  final case class Salt(value: String) extends AnyVal

  final case class EncryptedPassword(value: String) extends AnyVal

  final case class CreateUser(username: String, password: String)

  final case class LoginUser(
      username: String,
      password: String
  )

  final case class CreateUserForInsert(
      name: UserName,
      password: EncryptedPassword,
      salt: Salt,
      role: Role = Role.User
  )

  final case class UserWithPassword(
      id: UserId,
      name: UserName,
      password: EncryptedPassword,
      salt: Salt,
      role: Role = Role.User
  )

  object UserWithPassword {}

  final case class AdminUser(userName: UserName)
  final case class CommonUser(userName: UserName)

  final case class UserNameInUse(username: UserName)   extends NoStackTrace
  final case class InvalidPassword(username: UserName) extends NoStackTrace
}
