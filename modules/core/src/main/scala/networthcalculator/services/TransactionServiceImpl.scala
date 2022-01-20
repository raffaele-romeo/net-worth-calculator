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
import squants.market.Money
import squants.market.MoneyContext

object TransactionServiceImpl {
  def make[F[_]: MonadCancelThrow](transactor: Resource[F, HikariTransactor[F]]) =
    new TransactionsService[F] {

      override def create(userId: UserId, transaction: ValidTransaction): F[Unit] =
        transactor
          .use(
            TransactionQueries
              .insert(userId, transaction)
              .exceptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
                TransactionAlreadyCreated(
                  s"Transaction of ${transaction.month.toString}/${transaction.year.toInt} already inserted in the system"
                ).raiseError
              }
              .transact[F]
          )
          .void

      override def totalNetWorthByCurrencyYear(
          userId: UserId,
          year: Year
      )(using fxContext: MoneyContext): F[List[Money]] = transactor.use(
        TransactionQueries
        .calculateNetWorthByCurrencyYear(userId, year)
        .transact[F]
      )
    }
}

private object TransactionQueries {
  import MoneyImplicits.given
  
  def insert(userId: UserId, transaction: ValidTransaction): ConnectionIO[Int] = sql"""
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

    def calculateNetWorthByCurrencyYear(
        userId: UserId,
        year: Year
    )(using fxContext: MoneyContext): ConnectionIO[List[Money]] = sql"""
           | WITH relevant_transactions AS (
           | SELECT amount, currency, month, year, asset_name, asset_type,
           | ROW_NUMBER() OVER(PARTITION BY asset_name, asset_type, currency ORDER BY month DESC) AS rank
           | FROM transactions 
           | INNER JOIN assets ON transactions.asset_id = assets.id
           | WHERE transactions.user_id = ${userId.toLong} and year = ${year.toInt}
           | )
           | SELECT SUM(amount) as total, currency
           | FROM relevant_transactions
           | WHERE rank = 1
           | GROUP BY currency;
          """.stripMargin.query[Money].to[List]
    }
