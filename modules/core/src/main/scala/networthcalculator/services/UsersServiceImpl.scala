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
            UserNameInUse(user.username).raiseError[ConnectionIO, UserWithPassword]
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
         |  username,
         |  password,
         |  salt,
         |  role
         |)
         |VALUES (
         |  ${user.username.toString},
         |  ${user.password.toString},
         |  ${user.salt.toString},
         |  ${user.role.toString}
         |)
        """.stripMargin.update
      .withUniqueGeneratedKeys[UserWithPassword]("id", "username", "password", "salt", "role")

  def update(user: UserWithPassword): ConnectionIO[UserWithPassword] =
    sql"""
         |UPDATE users SET
         | username = ${user.username.toString},
         | password = ${user.password.toString},
         | salt = ${user.salt.toString}
         | role = ${user.role.toString}
         | where id = ${user.userId.toLong}
    """.stripMargin.update
      .withUniqueGeneratedKeys[UserWithPassword]("userId", "username", "password", "salt", "role")

  def select(userName: UserName): ConnectionIO[Option[UserWithPassword]] =
    sql"""
         | SELECT id, username, password, salt, role 
         | FROM users 
         | WHERE username = ${userName.toString}
    """.stripMargin.query[UserWithPassword].option

  def delete(userName: UserName): ConnectionIO[Int] =
    sql"""
         | DELETE FROM users
         | WHERE username = ${userName.toString}
    """.stripMargin.update.run
}
