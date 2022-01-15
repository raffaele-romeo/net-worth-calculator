package networthcalculator.domain

import networthcalculator.domain.assets._
import networthcalculator.domain.users.UserId
import networthcalculator.domain.assets.AssetId
import squants.market.{Currency, Money}
import squants.market.defaultMoneyContext

import io.circe._

import java.time.{Month, Year}

object transactions {
  opaque type TransactionId = Long
  object TransactionId {
    def fromLong(d: Long): TransactionId = d

    given Decoder[TransactionId] = Decoder.decodeLong
    given Encoder[TransactionId] = Encoder.encodeLong
  }

  opaque type Year = Int
  object Year {
    def apply(d: Int): Year = d

    given Decoder[Int] = Decoder.decodeInt
    given Encoder[Int] = Encoder.encodeInt
  }

  extension (x: Year) {
    def toInt: Int = x
  }

  final case class Transaction(
      transactionId: TransactionId,
      money: Money,
      month: Month,
      year: Year,
      assetId: AssetId,
      userId: UserId
  )

  final case class CreateTransaction(
      amount: BigDecimal,
      currency: String,
      month: String,
      year: Year,
      assetId: AssetId
  )

  final case class ValidTransaction(
      money: Money,
      month: Month,
      year: Year,
      assetId: AssetId
  )

  enum Month(val value: Int) {
    def getIntRepr = value

    case Jan  extends Month(1)
    case Feb  extends Month(2)
    case Mar  extends Month(3)
    case Apr  extends Month(4)
    case May  extends Month(5)
    case Jun  extends Month(6)
    case Jul  extends Month(7)
    case Aug  extends Month(8)
    case Sept extends Month(9)
    case Oct  extends Month(10)
    case Nov  extends Month(11)
    case Dec  extends Month(12)
  }

  object Month {
    def fromInt(s: Int): Month = s match {
      case 1  => Jan
      case 2  => Feb
      case 3  => Mar
      case 4  => Apr
      case 5  => May
      case 6  => Jun
      case 7  => Jul
      case 8  => Aug
      case 9  => Sept
      case 10 => Oct
      case 11 => Nov
      case 12 => Dec
    }

    def fromString(s: String): Month = Month.valueOf(s.toLowerCase.capitalize)

    given Decoder[Month] = Decoder[String].map(Month.fromString)
    given Encoder[Month] = Encoder[String].contramap(_.toString.capitalize)
  }

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

}
