package networthcalculator.algebras

import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{Password, Salt, UserName}

trait Auth[F[_]] {
  def newUser(username: UserName, password: Password): F[JwtToken]
  def login(username: UserName, password: Password, salt: Salt): F[JwtToken]
  def logout(token: JwtToken, username: UserName): F[Unit]
}

trait UsersAuth[F[_], A] {
  def findUser(token: JwtToken): F[Option[A]]
}
