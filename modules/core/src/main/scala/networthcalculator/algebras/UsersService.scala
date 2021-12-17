package networthcalculator.algebras

import networthcalculator.domain.users._

trait UsersService[F[_]] {
  def create(createUser: CreateUserForInsert): F[UserWithPassword]
  def find(userName: UserName): F[Option[UserWithPassword]]
}
