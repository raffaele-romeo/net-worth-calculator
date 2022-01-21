package networthcalculator.algebras

import networthcalculator.domain.transactions._
import networthcalculator.domain.users.UserId
import squants.market.{Money, MoneyContext}

import java.time.{Month, Year}

trait TransactionsService[F[_]] {
  def create(userId: UserId, transaction: List[ValidTransaction]): F[Unit]
  def totalNetWorthByCurrency(userId: UserId, year: Year)(using
      fxContext: MoneyContext
  ): F[List[Money]]
  def net

  // def totalNetWorthByYear(userId: UserId, year: Year, currency: Currency)(using fxContext: MoneyContext): F[List[Money]]
  // def delete(userId: UserId, transactionId: TransactionId): F[Unit]
  // def findAll(userId: UserId): F[List[Transaction]] // TODO Add pagination logic
}
