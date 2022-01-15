package networthcalculator.algebras

import networthcalculator.domain.transactions._
import networthcalculator.domain.users.UserId

trait TransactionsService[F[_]] {
  def create(userId: UserId, transaction: ValidTransaction): F[Unit]
//   def delete(userId: UserId, transactionId: TransactionId): F[Unit]
//   def findAll(userId: UserId): F[List[Transaction]] // When implementing, use doobie stream
//   def getTotalNetWorth(userId: UserId, totalNetWorth: FindTotalNetWorth): F[List[Statistics]]
//   def getTrendNetWorth(userId: UserId, trendNetWorth: FindTrendNetWorth): F[List[Statistics]]
}
