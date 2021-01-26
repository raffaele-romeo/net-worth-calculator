package networthcalculator.domain

import io.estatico.newtype.macros.newtype
import networthcalculator.domain.asset.{Asset, AssetId, AssetType}
import squants.market.{Currency, Money}
import java.time.Year
import java.time.Month

object transaction {

  @newtype case class TransactionId(value: Long)

  @newtype case class BankName(name: String)
  @newtype case class AccountName(name: String)

  case class Transaction(
      transactionId: TransactionId,
      bankName: BankName,
      accountName: Option[AccountName],
      asset: Asset,
      amount: Money,
      currency: Currency,
      month: Option[Month],
      year: Year
  )

  case class CreateTransaction(
      bankName: BankName,
      accountName: Option[AccountName],
      assetId: AssetId,
      amount: Money,
      currency: Currency,
      month: Option[Month],
      year: Year
  )

  case class UpdateTransaction(
      transactionId: TransactionId,
      amount: Money,
      currency: Currency
  )

  case class FindTotalNetWorth(
      month: Option[Month],
      year: Year,
      statisticsCurrencyType: Currency, //Output currency
      currency: Option[Currency], //If specified, get statistics by currency
      accountType: Option[AssetId], //If specified, get statistics by accountType
      accountTypeToExclude: List[AssetId] //Account to exclude from statistics
  )

  case class FindTrendNetWorth(
      monthFrom: Option[Month],
      yearFrom: Year,
      monthTo: Option[Month],
      yearTo: Year,
      statisticsCurrencyType: Currency,
      currency: Option[Currency],
      assetType: Option[AssetType],
      assetTypesToExclude: List[AssetType] //Query parameter of string with comma separator. Needs to be splitted
  )

  case class Statistics(
      assetType: Option[AssetType],
      currency: Currency,
      amount: Money
  )

}
