// package networthcalculator.domain

// import networthcalculator.domain.accounts._
// import squants.market.{Currency, Money}

// import java.time.{Month, Year}

// object transactions {
//   opaque type TransactionId = Long
//   object TransactionId {
//     def fromLong(d: Long): TransactionId = d
//   }

//   opaque type BankName = String
//   object BankName {
//     def fromString(d: String): BankName = d
//   }

//   opaque type AccountName = String
//   object AccountName {
//     def fromString(d: String): AccountName = d
//   }

//   final case class Transaction(
//       transactionId: TransactionId,
//       bankName: BankName,
//       accountName: Option[AccountName],
//       asset: Asset,
//       amount: Money,
//       currency: Currency,
//       month: Option[Month],
//       year: Year
//   )

//   final case class CreateTransaction(
//       bankName: BankName,
//       accountName: Option[AccountName],
//       assetId: AssetId,
//       amount: Money,
//       currency: Currency,
//       month: Option[Month],
//       year: Year
//   )

//   final case class UpdateTransaction(
//       transactionId: TransactionId,
//       amount: Money,
//       currency: Currency
//   )

//   final case class FindTotalNetWorth(
//       month: Option[Month],
//       year: Year,
//       statisticsCurrencyType: Currency,   // Output currency
//       currency: Option[Currency],         // If specified, get statistics by currency
//       accountType: Option[AssetId],       // If specified, get statistics by accountType
//       accountTypeToExclude: List[AssetId] // Account to exclude from statistics
//   )

//   final case class FindTrendNetWorth(
//       monthFrom: Option[Month],
//       yearFrom: Year,
//       monthTo: Option[Month],
//       yearTo: Year,
//       statisticsCurrencyType: Currency,
//       currency: Option[Currency],
//       assetType: Option[AssetType],
//       assetTypesToExclude: List[
//         AssetType
//       ] // Query parameter of string with comma separator. Needs to be split
//   )

//   final case class Statistics(
//       assetType: Option[AssetType],
//       currency: Currency,
//       amount: Money
//   )

// }
