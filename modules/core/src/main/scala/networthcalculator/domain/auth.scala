package networthcalculator.domain

import cats.Eq
import networthcalculator.domain.auth.Role.{Admin, Customer}
import networthcalculator.domain.users.{User, UserName}
import networthcalculator.effects._
import tsec.authentication._
import tsec.authorization._
import tsec.mac.jca.HMACSHA256

object auth {

  def AdminRequired[F[_]: MonadThrow]: BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, UserName]] =
    BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, UserName]](Admin)

  def CustomerRequired[F[_]: MonadThrow]: BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, UserName]] =
    BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, UserName]](Customer)

  def CustomerOrAdminRequired[F[_]: MonadThrow]: BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, UserName]] =
    BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, UserName]](Customer, Admin)

  sealed case class Role(roleRepr: String)

  object Role extends SimpleAuthEnum[Role, String] {

    val Admin: Role = Role("Admin")
    val Customer: Role = Role("User")

    implicit val E: Eq[Role] = Eq.fromUniversalEquals[Role]
    protected val values: AuthGroup[Role] = AuthGroup(Admin, Customer)

    def getRepr(t: Role): String = t.roleRepr
  }

}
