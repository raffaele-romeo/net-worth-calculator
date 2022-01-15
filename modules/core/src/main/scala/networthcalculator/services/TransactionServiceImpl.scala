package networthcalculator.services

import networthcalculator.algebras.TransactionsService

import cats.effect.{Resource, MonadCancelThrow}
import cats.implicits.*
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.postgres.*
import networthcalculator.domain.transactions.*
import networthcalculator.domain.users.*

object TransactionServiceImpl {
  def make[F[_]: MonadCancelThrow](transactor: Resource[F, HikariTransactor[F]]) =
    new TransactionsService[F] {

      override def create(userId: UserId, transaction: ValidTransaction): F[Unit] =
        transactor
          .use(
            TransactionQueries
              .insert(userId, transaction)
              .transact[F]
          )
          .void
    }
}

private object TransactionQueries {
  def insert(userId: UserId, transaction: ValidTransaction): ConnectionIO[Int] =
    sql"""
       | INSERT INTO transactions (
       | amount,
       | currency,
       | month,
       | year,
       | asset_id,
       | user_id
       | )
       | VALUES (
       | ${transaction.money.amount},
       | ${transaction.money.currency.toString},
       | ${transaction.month.getIntRepr},
       | ${transaction.year.toInt},
       | ${transaction.assetId.toLong},
       | ${userId.toLong}
       | )
         """.stripMargin.update.run
}
