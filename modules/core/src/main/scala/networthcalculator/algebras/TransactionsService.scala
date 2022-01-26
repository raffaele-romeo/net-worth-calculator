package networthcalculator.algebras

import networthcalculator.domain.assets._
import networthcalculator.domain.transactions._
import networthcalculator.domain.users.UserId
import squants.market.{Money, MoneyContext}

import java.time.{Month, Year}

trait TransactionsService[F[_]] {
  def create(userId: UserId, transaction: List[ValidTransaction]): F[Unit]

  def delete(userId: UserId, transactionId: TransactionId): F[Unit]

  // TODO Add pagination logic to all the methods that retrieve a list
  def totalNetWorthByCurrency(userId: UserId, year: Option[Year]): F[List[TotalNetWorthByCurrency]]

  def netWorthByCurrencyAndAsset(
      userId: UserId,
      assetId: AssetId,
      year: Option[Year]
  ): F[List[TotalNetWorthByCurrency]]

  def netWorthByCurrencyAndAssetType(
      userId: UserId,
      assetType: AssetType,
      year: Option[Year]
  ): F[List[TotalNetWorthByCurrency]]

  def findAll(userId: UserId): F[List[Transaction]]
}
