package networthcalculator.domain

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval.Closed
import io.estatico.newtype.macros.newtype
import networthcalculator.domain.asset.{Asset, AssetId, AssetType}
import squants.market.{Currency, Money}

object transaction {

  type MonthValue = Int Refined Closed[1, 12]

  @newtype case class TransactionId(value: Long)

  @newtype case class Month(value: Option[Int])
  @newtype case class Year(value: Int)

  @newtype case class BankName(name: String)
  @newtype case class AccountName(name: String)

  case class Transaction(
                          transactionId: TransactionId,
                          bankName: BankName,
                          accountName: Option[AccountName],
                          asset: Asset,
                          amount: Money,
                          currency: Currency,
                          month: Month,
                          year: Year
  )

  case class CreateTransaction(
                                bankName: BankName,
                                accountName: Option[AccountName],
                                assetId: AssetId,
                                amount: Money,
                                currency: Currency,
                                month: Month,
                                year: Year
  )

  case class UpdateTransaction(
      transactionId: TransactionId,
      amount: Money,
      currency: Currency
  )

  case class FindTotalNetWorth(
      month: Month,
      year: Year,
      statisticsCurrencyType: Currency,
      currency: Option[Currency],
      accountType: Option[AssetId],
      accountTypeToExclude: List[AssetId]
  )

  case class FindTrendNetWorth(
                                monthFrom: Month,
                                yearFrom: Year,
                                monthTo: Month,
                                yearTo: Year,
                                statisticsCurrencyType: Currency,
                                currency: Option[Currency],
                                assetType: Option[AssetType],
                                assetTypesToExclude: List[AssetType]
  )

  case class Statistics(
      assetType: Option[AssetType],
      currency: Currency,
      amount: Money
  )

}
