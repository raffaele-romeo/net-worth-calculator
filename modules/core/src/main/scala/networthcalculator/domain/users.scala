package networthcalculator.domain

import doobie.util.{Get, Put, Read, Write}
import networthcalculator.domain.auth.Role

import scala.util.control.NoStackTrace
import cats.Show
import scala.annotation.targetName

object users {

  opaque type UserId = Long

  object UserId {
    def apply(d: Long): UserId = d
  }

  extension (x: UserId) {
    def toLong: Long = x
  }

  opaque type UserName = String

  object UserName {
    def apply(d: String): UserName = d
  }

  extension (x: UserName) {
    @targetName("UserName")
    def toString: String = x
  }

  opaque type Password = String

  object Password {
    def apply(d: String): Password = d
  }

  extension (x: Password) {
    @targetName("Password")
    def toString: String = x
  }

  opaque type Salt = String

  object Salt {
    def apply(d: String): Salt = d
  }

  extension (x: Salt) {
    @targetName("Salt")
    def toString: String = x
  }

  opaque type EncryptedPassword = String

  object EncryptedPassword {
    def apply(d: String): EncryptedPassword = d
  }

  extension (x: EncryptedPassword) {
    @targetName("EncryptedPassword")
    def toString: String = x
  }

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
      username: UserName,
      password: EncryptedPassword,
      salt: Salt,
      role: Role = Role.User
  )

  final case class UserWithPassword(
      userId: UserId,
      username: UserName,
      password: EncryptedPassword,
      salt: Salt,
      role: Role = Role.User
  )

  object UserWithPassword {
    implicit val userWithPasswordRead: Read[UserWithPassword] =
      Read[(Long, String, String, String, String)].map {
        case (id, username, password, salt, role) =>
          UserWithPassword(
            UserId(id),
            UserName(username),
            EncryptedPassword(password),
            Salt(salt),
            Role.fromString(role)
          )
      }
    implicit val userWithPasswordWrite: Write[UserWithPassword] =
      Write[(Long, String, String, String, String)].contramap { userWithPassword =>
        (
          userWithPassword.userId.toLong,
          userWithPassword.username.toString,
          userWithPassword.password.toString,
          userWithPassword.salt.toString,
          userWithPassword.role.toString
        )
      }
  }

  final case class AdminUser(userName: UserName)
  object AdminUser {
    given showAdminUser: Show[AdminUser] = Show.show(_.userName)
  }

  final case class CommonUser(userName: UserName)
  object CommonUser {
    given showCommonUser: Show[CommonUser] = Show.show(_.userName)
  }

  final case class UserNameInUse(username: UserName)   extends NoStackTrace
  final case class InvalidPassword(username: UserName) extends NoStackTrace
}
