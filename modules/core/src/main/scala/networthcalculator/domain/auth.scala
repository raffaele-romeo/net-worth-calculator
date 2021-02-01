package networthcalculator.domain

import cats.Eq
import networthcalculator.domain.auth.Role.{Administrator, Customer}
import networthcalculator.domain.users.{User, UserName}
import networthcalculator.effects._
import tsec.authentication._
import tsec.authorization._
import tsec.mac.jca.HMACSHA256

object auth {

  def AdminRequired[F[_]: MonadThrow]: BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, UserName]] =
    BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, UserName]](Administrator)

  def CustomerRequired[F[_]: MonadThrow]: BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, UserName]] =
    BasicRBAC[F, Role, User, AugmentedJWT[HMACSHA256, UserName]](Customer)

  sealed case class Role(roleRepr: String)

  object Role extends SimpleAuthEnum[Role, String] {

    val Administrator: Role = Role("Administrator")
    val Customer: Role = Role("User")

    implicit val E: Eq[Role] = Eq.fromUniversalEquals[Role]
    protected val values: AuthGroup[Role] = AuthGroup(Administrator, Customer)

    def getRepr(t: Role): String = t.roleRepr
  }

}
