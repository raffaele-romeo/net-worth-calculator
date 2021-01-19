package networthcalculator.algebras

import networthcalculator.domain.auth.{UserId, UserName}

trait Users[F[_]] {
  def newUser(username: UserName): F[UserId]
}
