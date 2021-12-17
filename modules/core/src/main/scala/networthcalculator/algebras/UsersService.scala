package networthcalculator.algebras

import cats.data.OptionT
import networthcalculator.domain.users._

trait UsersService[F[_]] {
  def create(createUser: CreateUserForInsert): F[UserWithPassword]
  def find(userName: UserName): OptionT[F, UserWithPassword]
}