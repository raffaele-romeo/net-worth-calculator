package networthcalculator.domain

import networthcalculator.domain.asset._
import squants.market.{Currency, Money}

import java.time.{Month, Year}

object transaction {

  final case class TransactionId(value: Long)

  final case class BankName(name: String)
  final case class AccountName(name: String)

  final case class Transaction(
      transactionId: TransactionId,
      bankName: BankName,
      accountName: Option[AccountName],
      asset: Asset,
      amount: Money,
      currency: Currency,
      month: Option[Month],
      year: Year
  )

  final case class CreateTransaction(
      bankName: BankName,
      accountName: Option[AccountName],
      assetId: AssetId,
      amount: Money,
      currency: Currency,
      month: Option[Month],
      year: Year
  )

  final case class UpdateTransaction(
      transactionId: TransactionId,
      amount: Money,
      currency: Currency
  )

  final case class FindTotalNetWorth(
      month: Option[Month],
      year: Year,
      statisticsCurrencyType: Currency,   // Output currency
      currency: Option[Currency],         // If specified, get statistics by currency
      accountType: Option[AssetId],       // If specified, get statistics by accountType
      accountTypeToExclude: List[AssetId] // Account to exclude from statistics
  )

  final case class FindTrendNetWorth(
      monthFrom: Option[Month],
      yearFrom: Year,
      monthTo: Option[Month],
      yearTo: Year,
      statisticsCurrencyType: Currency,
      currency: Option[Currency],
      assetType: Option[AssetType],
      assetTypesToExclude: List[
        AssetType
      ] // Query parameter of string with comma separator. Needs to be split
  )

  final case class Statistics(
      assetType: Option[AssetType],
      currency: Currency,
      amount: Money
  )

}
