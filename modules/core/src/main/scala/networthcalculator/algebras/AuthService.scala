package networthcalculator.algebras

import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{Password, UserName, ValidUser}

trait AuthService[F[_]] {
  def newUser(validUser: ValidUser): F[JwtToken]
  def login(validUser: ValidUser): F[JwtToken]
  def validate(username: String, password: String): F[ValidUser]
}

trait UsersAuthService[F[_], A] {
  def findUser(token: JwtToken): F[Option[A]]
}
