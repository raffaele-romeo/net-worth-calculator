package networthcalculator.algebras

import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.hikari._
import doobie.implicits._
import networthcalculator.domain.auth._
import networthcalculator.effects.BracketThrow
import tsec.authentication.BackingStore

object LiveUsers {

  def make[F[_]: Sync](transactor: Resource[F, HikariTransactor[F]]): F[LiveUsers[F]] = {
    Sync[F]
      .delay {
        new LiveUsers[F](transactor)
      }
  }
}

final class LiveUsers[F[_]: BracketThrow: Sync] private (
    transactor: Resource[F, HikariTransactor[F]]
) extends BackingStore[F, UserId, User] {

  override def put(user: User): F[User] = {
    transactor
      .use(
        UserQueries
          .insert(user)
          .transact[F]
      )
  }

  override def update(user: User): F[User] = {
    transactor.use(
      UserQueries
        .update(user)
        .transact[F]
    )
  }

  override def delete(userId: UserId): F[Unit] = {
    transactor.use(
      UserQueries
        .delete(userId)
        .transact[F]
    ) *> Sync[F].unit
  }

  override def get(userId: UserId): OptionT[F, User] = {
    OptionT(
      transactor.use(
        UserQueries
          .select(userId)
          .transact[F]
      )
    )
  }
}

private object UserQueries {

  import networthcalculator.ext.doobienewtype._

  def insert(user: User): ConnectionIO[User] = {
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
      .handleErrorWith {
        case ex: java.sql.SQLException if ex.getErrorCode == 23505 =>
          UserNameInUse(user.name).raiseError[ConnectionIO, User]
      }
  }

  def update(user: User): ConnectionIO[User] = {
    sql"""
         |UPDATE users SET
         | name = ${user.name.value},
         | password = ${user.password.value},
         | salt = ${user.salt.value}
         | where id = ${user.id.value}
    """.stripMargin.update
      .withUniqueGeneratedKeys[User]("id", "name", "password", "password", "role")
  }

  def select(userId: UserId): ConnectionIO[Option[User]] = {
    sql"""
         | SELECT id, name, password, salt, role 
         | FROM users 
         | WHERE id = ${userId.value}
    """.stripMargin.query[User].option
  }

  def delete(userId: UserId): ConnectionIO[Int] = {
    sql"""
         | DELETE FROM users 
         | WHERE id=${userId.value}
    """.stripMargin.update.run
  }

}
