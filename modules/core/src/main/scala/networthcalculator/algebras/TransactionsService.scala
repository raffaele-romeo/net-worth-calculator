package networthcalculator.algebras

import networthcalculator.domain.assets.*
import networthcalculator.domain.transactions.*
import networthcalculator.domain.users.UserId
import squants.market.{ Money, MoneyContext }

import java.time.{ Month, Year }

trait TransactionsService[F[_]]:
  def create(userId: UserId, transaction: List[ValidTransaction]): F[Unit]

  def delete(userId: UserId, transactionId: TransactionId): F[Unit]

  // TODO Add pagination logic to all the methods that retrieve a list
  def totalNetWorthByCurrency(
    userId: UserId,
    year: Option[Year]
  ): F[List[AggregatedTransactions]]

  def findTransactionsByAssetId(
    userId: UserId,
    assetId: AssetId,
    year: Option[Year]
  ): F[List[AggregatedTransactions]]

  def findTransactionsByAssetType(
    userId: UserId,
    assetType: AssetType,
    year: Option[Year]
  ): F[List[AggregatedTransactions]]

  def findAll(userId: UserId): F[List[Transaction]]
