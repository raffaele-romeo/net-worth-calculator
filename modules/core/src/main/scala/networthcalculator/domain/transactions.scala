package networthcalculator.domain

import doobie.util.{Read, Write}
import io.circe._
import io.circe.generic.semiauto._
import networthcalculator.domain.assets._
import networthcalculator.domain.users.UserId
import squants.market._
import squants.market.defaultMoneyContext
import java.time.{Month, Year}
import scala.util.control.NoStackTrace

object transactions {
  opaque type TransactionId = Long
  object TransactionId {
    def apply(d: Long): TransactionId = d

    given Decoder[TransactionId] = Decoder.decodeLong
    given Encoder[TransactionId] = Encoder.encodeLong
  }

  extension (x: TransactionId) {
    def toLong: Long = x
  }

  final case class Transaction(
      transactionId: TransactionId,
      money: Money,
      month: Month,
      year: Year,
      assetId: AssetId,
      userId: UserId
  )

  object Transaction {
    given transactionRead: Read[Transaction] =
      Read[(Long, BigDecimal, String, Int, Int, Long, Long)].map {
        case (id, total, currency, month, year, assetId, userId) =>
          Transaction(
            TransactionId(id),
            Money(
              total,
              currency
            )(defaultMoneyContext).get,
            Month.of(month),
            Year.of(year),
            AssetId(assetId),
            UserId(userId)
          )
      }

    given Encoder[Transaction] = new Encoder[Transaction] {
      final def apply(transaction: Transaction): Json = Json.obj(
        ("id", Json.fromLong(transaction.transactionId.toLong)),
        ("amount", Json.fromBigDecimal(transaction.money.amount)),
        ("currency", Json.fromString(transaction.money.currency.code)),
        ("month", Json.fromInt(transaction.month.getValue)),
        ("year", Json.fromInt(transaction.year.getValue)),
        ("assetId", Json.fromLong(transaction.assetId.toLong)),
        ("userId", Json.fromLong(transaction.userId.toLong))
      )
    }
  }

  final case class CreateTransaction(
      month: Int,
      year: Year,
      transactions: List[TransactionValue]
  )

  final case class TransactionValue(amount: BigDecimal, currency: String, assetId: AssetId)

  final case class TotalNetWorthByCurrency(
      total: Money,
      month: Month,
      year: Year
  )
  final case class ExplodeCreateTransaction(
      amount: BigDecimal,
      currency: String,
      month: Int,
      year: Year,
      assetId: AssetId
  )

  final case class ValidTransaction(
      money: Money,
      month: Month,
      year: Year,
      assetId: AssetId
  )

  object TotalNetWorthByCurrency {
    given totalNetWorthByCurrencyRead(using
        fxContext: MoneyContext
    ): Read[TotalNetWorthByCurrency] =
      Read[(BigDecimal, String, Int, Int)].map { case (total, currency, month, year) =>
        TotalNetWorthByCurrency(
          Money(
            total,
            currency
          ).get,
          Month.of(month),
          Year.of(year)
        )
      }

    given Encoder[TotalNetWorthByCurrency] = new Encoder[TotalNetWorthByCurrency] {
      final def apply(totalNetWorthByCurrency: TotalNetWorthByCurrency): Json = Json.obj(
        ("amount", Json.fromBigDecimal(totalNetWorthByCurrency.total.amount)),
        ("currency", Json.fromString(totalNetWorthByCurrency.total.currency.code)),
        ("month", Json.fromInt(totalNetWorthByCurrency.month.getValue)),
        ("year", Json.fromInt(totalNetWorthByCurrency.year.getValue))
      )
    }
  }

  final case class TransactionAlreadyCreated(error: String) extends NoStackTrace

}
