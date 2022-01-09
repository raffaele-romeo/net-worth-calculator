package networthcalculator.services

import cats.effect.{Resource, Sync}
import cats.implicits.*
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import networthcalculator.algebras.AccountsService
import networthcalculator.domain.accounts.*
import doobie.postgres.*
import networthcalculator.domain.users.UserId
import org.typelevel.log4cats.Logger
import doobie.util.log.LogHandler

object AccountsServiceImpl {
  def make[F[_]](
      transactor: Resource[F, HikariTransactor[F]]
  )(using S: Sync[F], L: Logger[F]): AccountsService[F] = new AccountsService[F] {

    given LogHandler = LogHandler.jdkLogHandler

    override def findAll(userId: UserId): F[List[Account]] =
      transactor
        .use(
          AccountsQueries
            .select(userId)
            .transact[F]
        )

    override def create(createAccount: CreateAccount, userId: UserId): F[Unit] =
      transactor
        .use(
          AccountsQueries
            .insert(createAccount, userId)
            .transact[F]
        )
        .void

    override def delete(accountId: AccountId, userId: UserId): F[Unit] =
      transactor
        .use(
          AccountsQueries
            .delete(accountId, userId)
            .transact[F]
        )
        .void
  }
}

private object AccountsQueries {

  def insert(createAccount: CreateAccount, userId: UserId): ConnectionIO[Int] =
    sql"""
         | INSERT INTO accounts (
         | account_type,
         | account_name,
         | user_id
         | )
         | VALUES (
         | ${createAccount.accountType.toString.toLowerCase},
         | ${createAccount.accountName.toString.toLowerCase},
         | ${userId.toLong}
         | )
         """.stripMargin.update.run

  def select(userId: UserId): ConnectionIO[List[Account]] =
    sql"""
         | SELECT id, account_type, account_name, user_id
         | FROM accounts
         | WHERE user_id = ${userId.toLong}
         """.stripMargin.query[Account].to[List]

  def delete(accountId: AccountId, userId: UserId): ConnectionIO[Int] =
    sql"""
         | DELETE FROM accounts
         | WHERE id = ${accountId.toLong} AND user_id = ${userId.toLong}
         """.stripMargin.update.run
}
