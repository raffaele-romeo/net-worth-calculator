package networthcalculator.algebras

import networthcalculator.domain.transactions._
import networthcalculator.domain.users.UserId
import squants.market.{Money, MoneyContext}

trait TransactionsService[F[_]] {
  def create(userId: UserId, transaction: ValidTransaction): F[Unit]
  def totalNetWorthByCurrencyYear(userId: UserId, year: Year)(using fxContext: MoneyContext): F[List[Money]]

  // def totalNetWorthByYear(userId: UserId, year: Year, currency: Currency)(using fxContext: MoneyContext): F[List[Money]]
  // def delete(userId: UserId, transactionId: TransactionId): F[Unit]
  // def findAll(userId: UserId): F[List[Transaction]] // TODO Add pagination logic
}
