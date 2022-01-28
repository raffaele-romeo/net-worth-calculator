package networthcalculator.domain

import cats.Show
import doobie.util.{ Read, Write }
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import networthcalculator.domain.auth.Role

import scala.annotation.targetName
import scala.util.control.NoStackTrace

object users:

  opaque type UserId = Long

  object UserId:
    def apply(d: Long): UserId = d

    given Decoder[UserId] = Decoder.decodeLong
    given Encoder[UserId] = Encoder.encodeLong

  extension (x: UserId) def toLong: Long = x

  opaque type UserName = String

  object UserName:
    def apply(d: String): UserName = d

    given Decoder[UserName] = Decoder.decodeString
    given Encoder[UserName] = Encoder.encodeString

  extension (x: UserName)
    @targetName("UserName")
    def toString: String = x

  opaque type Password = String

  object Password:
    def apply(d: String): Password = d
    given Decoder[Password]        = Decoder.decodeString
    given Encoder[Password]        = Encoder.encodeString

  extension (x: Password)
    @targetName("Password")
    def toString: String = x

  opaque type Salt = String

  object Salt:
    def apply(d: String): Salt = d

  extension (x: Salt)
    @targetName("Salt")
    def toString: String = x

  opaque type EncryptedPassword = String

  object EncryptedPassword:
    def apply(d: String): EncryptedPassword = d

  extension (x: EncryptedPassword)
    @targetName("EncryptedPassword")
    def toString: String = x

  final case class CreateUser(username: UserName, password: Password)

  final case class LoginUser(username: UserName, password: Password)

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

  object UserWithPassword:
    given userWithPasswordRead: Read[UserWithPassword] =
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
    given userWithPasswordWrite: Write[UserWithPassword] =
      Write[(Long, String, String, String, String)].contramap {
        userWithPassword =>
          (
            userWithPassword.userId.toLong,
            userWithPassword.username.toString,
            userWithPassword.password.toString,
            userWithPassword.salt.toString,
            userWithPassword.role.toString
          )
      }

  final case class AdminUser(userName: UserName)
  object AdminUser:
    given showAdminUser: Show[AdminUser] = Show.show(_.userName)

  final case class CommonUser(userId: UserId, userName: UserName)
      derives Encoder.AsObject
  object CommonUser:
    given showCommonUser: Show[CommonUser] = Show.show(_.asJson.toString)

  final case class UserNameInUse(username: UserName)   extends NoStackTrace
  final case class InvalidPassword(username: UserName) extends NoStackTrace
