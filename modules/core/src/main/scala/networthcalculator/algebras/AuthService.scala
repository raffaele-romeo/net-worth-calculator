package networthcalculator.algebras

import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{Password, UserName}

trait AuthService[F[_]] {
  def newUser(username: UserName, password: Password): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
}

trait UsersAuthService[F[_], A] {
  def findUser(token: JwtToken): F[Option[A]]
}
