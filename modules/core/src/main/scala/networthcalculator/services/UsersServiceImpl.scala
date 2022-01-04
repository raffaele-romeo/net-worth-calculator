package networthcalculator.services

import cats.effect.{MonadCancelThrow, Resource}
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import networthcalculator.algebras.UsersService
import networthcalculator.domain.users.{
  CreateUserForInsert,
  UserName,
  UserNameInUse,
  UserWithPassword
}
import doobie.postgres.*

object UsersServiceImpl {
  def make[F[_]: MonadCancelThrow](
      transactor: Resource[F, HikariTransactor[F]]
  ): UsersService[F] = new UsersService[F] {

    override def create(user: CreateUserForInsert): F[UserWithPassword] =
      transactor.use(
        UserQueries
          .insert(user)
          .exceptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
            UserNameInUse(user.name).raiseError[ConnectionIO, UserWithPassword]
          }
          .transact[F]
      )

    override def find(userName: UserName): F[Option[UserWithPassword]] =
      transactor.use(
        UserQueries
          .select(userName)
          .transact[F]
      )
  }
}
private object UserQueries {
  import UserWithPassword.given // This is needed to avoid error "Cannot find or construct a Read instance for type: UserWithPassword"

  def insert(user: CreateUserForInsert): ConnectionIO[UserWithPassword] =
    sql"""
         |INSERT INTO users (
         |  name,
         |  password,
         |  salt,
         |  role
         |)
         |VALUES (
         |  ${user.name.value},
         |  ${user.password.value},
         |  ${user.salt.value},
         |  ${user.role.toString}
         |)
        """.stripMargin.update
      .withUniqueGeneratedKeys[UserWithPassword]("id", "name", "password", "salt", "role")

  def update(user: UserWithPassword): ConnectionIO[UserWithPassword] =
    sql"""
         |UPDATE users SET
         | name = ${user.name.value},
         | password = ${user.password.value},
         | salt = ${user.salt.value}
         | role = ${user.role.toString}
         | where id = ${user.id.value}
    """.stripMargin.update
      .withUniqueGeneratedKeys[UserWithPassword]("id", "name", "password", "salt", "role")

  def select(userName: UserName): ConnectionIO[Option[UserWithPassword]] =
    sql"""
         | SELECT id, name, password, salt, role 
         | FROM users 
         | WHERE name = ${userName.value}
    """.stripMargin.query[UserWithPassword].option

  def delete(userName: UserName): ConnectionIO[Int] =
    sql"""
         | DELETE FROM users 
         | WHERE name = ${userName.value}
    """.stripMargin.update.run

}
