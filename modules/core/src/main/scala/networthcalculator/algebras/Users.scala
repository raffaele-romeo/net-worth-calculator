package networthcalculator.algebras

import cats.data.OptionT
import networthcalculator.domain.users._

trait Users[F[_]] {
  def create(createUser: CreateUserForInsert): F[User]
  def get(userName: UserName): OptionT[F, User]
}