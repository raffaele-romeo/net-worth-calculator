package networthcalculator.algebras

import networthcalculator.domain.auth.{ User, UserId, UserName }

trait Users[F[_]] {
  def find(username: UserName): F[Option[User]]
  def newUser(username: UserName): F[UserId]
}
