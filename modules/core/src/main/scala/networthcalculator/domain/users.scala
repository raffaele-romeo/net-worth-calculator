package networthcalculator.domain

import doobie.util.{Get, Put, Read, Write}
import networthcalculator.domain.auth.Role

import scala.util.control.NoStackTrace
import cats.Show

object users {

  final case class UserId(value: Long)

  final case class UserName(value: String)

  final case class Password(value: String)

  // --------- user registration -----------

  final case class Salt(value: String)

  final case class EncryptedPassword(value: String)

  final case class CreateUser(username: String, password: String)

  final case class LoginUser(
      username: String,
      password: String
  )

  final case class ValidUser(
      username: UserName,
      password: Password
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

  object UserWithPassword {
    implicit val roleGet: Get[Role]     = Get[String].tmap(fromString)
    implicit val rolePut: Put[Role]     = Put[String].tcontramap(toString)
    implicit val roleRead: Read[Role]   = Read[String].map(fromString)
    implicit val roleWrite: Write[Role] = Write[String].contramap(toString)

    private def fromString(s: String): Role = {
      if (s == Role.User.roleRepr) Role.User
      else Role.Admin
    }

    private def toString(r: Role) = r match {
      case role @ (Role.Admin | Role.User) => role.roleRepr
    }
  }

  final case class AdminUser(userName: UserName)
  object AdminUser {
    implicit val showAdminUser: Show[AdminUser] = Show.show(_.userName.value)
  }

  final case class CommonUser(userName: UserName)
  object CommonUser {
    implicit val showCommonUser: Show[CommonUser] = Show.show(_.userName.value)
  }

  final case class UserNameInUse(username: UserName)   extends NoStackTrace
  final case class InvalidPassword(username: UserName) extends NoStackTrace
}
