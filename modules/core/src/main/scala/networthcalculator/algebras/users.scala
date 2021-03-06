package networthcalculator.algebras

import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.hikari._
import doobie.implicits._
import networthcalculator.domain.users._
import networthcalculator.effects.BracketThrow
import tsec.authentication.IdentityStore

trait Users[F[_]] {
  def create(createUser: CreateUserForInsert): F[User]
}

object LiveUsers {

  def make[F[_]: Sync](transactor: Resource[F, HikariTransactor[F]]): F[LiveUsers[F]] = {
    Sync[F]
      .delay {
        new LiveUsers[F](transactor)
      }
  }
}

final class LiveUsers[F[_]: BracketThrow] private (
    transactor: Resource[F, HikariTransactor[F]]
) extends IdentityStore[F, UserName, User]
    with Users[F] {

  override def create(user: CreateUserForInsert): F[User] =
    transactor.use(
      UserQueries
        .insert(user)
        .handleErrorWith {
          case ex: java.sql.SQLException if ex.getErrorCode == 23505 =>
            UserNameInUse(user.name).raiseError[ConnectionIO, User]
        }
        .transact[F]
    )

  override def get(userName: UserName): OptionT[F, User] =
    OptionT(
      transactor.use(
        UserQueries
          .select(userName)
          .transact[F]
      )
    )
}

private object UserQueries {

  import networthcalculator.ext.CoercibleDoobieCodec._

  def insert(user: CreateUserForInsert): ConnectionIO[User] =
    sql"""
         |INSERT INTO users (
         |  name,
         |  password,
         |  salt,
         |  role
         |)
         |VALUES (
         |  ${user.name.value}
         |  ${user.password.value}
         |  ${user.salt.value}
         |  ${user.role.roleRepr}
         |)
        """.stripMargin.update
      .withUniqueGeneratedKeys[User]("id", "name", "password", "salt", "role")

  def update(user: User): ConnectionIO[User] =
    sql"""
         |UPDATE users SET
         | name = ${user.name.value},
         | password = ${user.password.value},
         | salt = ${user.salt.value}
         | where id = ${user.id.value}
    """.stripMargin.update
      .withUniqueGeneratedKeys[User]("id", "name", "password", "password", "role")

  def select(userName: UserName): ConnectionIO[Option[User]] =
    sql"""
         | SELECT id, name, password, salt, role 
         | FROM users 
         | WHERE name = ${userName.value}
    """.stripMargin.query[User].option

  def delete(userName: UserName): ConnectionIO[Int] =
    sql"""
         | DELETE FROM users 
         | WHERE name = ${userName.value}
    """.stripMargin.update.run

}
