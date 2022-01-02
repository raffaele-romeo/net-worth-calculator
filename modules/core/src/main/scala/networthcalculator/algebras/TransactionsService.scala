package networthcalculator.algebras

import networthcalculator.domain.transaction._
import networthcalculator.domain.users.UserId

trait TransactionsService[F[_]] {
  def insert(userId: UserId, transaction: CreateTransaction): F[TransactionId]
  def bulkInsert(userId: UserId, transactions: List[CreateTransaction]): F[Unit]
  def update(userId: UserId, updateTransaction: UpdateTransaction): F[TransactionId]
  def delete(userId: UserId, transactionId: TransactionId): F[Unit]
  def findAll(userId: UserId): F[List[Transaction]] // When implementing, use doobie stream
  def getTotalNetWorth(userId: UserId, totalNetWorth: FindTotalNetWorth): F[List[Statistics]]
  def getTrendNetWorth(userId: UserId, trendNetWorth: FindTrendNetWorth): F[List[Statistics]]
}
