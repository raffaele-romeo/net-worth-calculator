package networthcalculator.domain

import cats.{Eq, MonadError}
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import networthcalculator.domain.auth.Role.{Administrator, Customer}
import networthcalculator.effects._
import tsec.authentication._
import tsec.authorization._
import tsec.mac.jca.HMACSHA256

import scala.util.control.NoStackTrace

object auth {

  def AdminRequired[F[_]: MonadThrow]: BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, Int]] =
    BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, Int]](Administrator)

  def CustomerRequired[F[_]: MonadThrow]: BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, Int]] =
    BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, Int]](Customer)

  // Role definition
  sealed case class Role(roleRepr: String)

  @newtype case class UserId(value: Long)

  @newtype case class UserName(value: String)

  @newtype case class Password(value: String)

  // --------- user registration -----------

  @newtype case class Salt(value: String)

  @newtype case class EncryptedPassword(value: String)

  case class User(id: UserId, name: UserName, password: Password, salt: Salt, role: Role = Role.Customer)

  @newtype case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = UserName(value.value.toLowerCase())
  }

  @newtype case class PasswordParam(value: NonEmptyString) {
    def toDomain: Password = Password(value.value)
  }

  case class CreateUser(username: UserNameParam, password: PasswordParam)

  case class UserNameInUse(username: UserName) extends NoStackTrace

  object Role extends SimpleAuthEnum[Role, String] {

    val Administrator: Role = Role("Administrator")
    val Customer: Role = Role("User")

    implicit val E: Eq[Role] = Eq.fromUniversalEquals[Role]
    protected val values: AuthGroup[Role] = AuthGroup(Administrator, Customer)

    def getRepr(t: Role): String = t.roleRepr
  }

  object User {

    implicit def authRole[F[_]](implicit F: MonadError[F, Throwable]): AuthorizationInfo[F, Role, User] =
      (u: User) => F.pure(u.role)
  }

}
