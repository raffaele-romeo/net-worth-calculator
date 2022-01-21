package networthcalculator.domain

import doobie.util.{Read, Write}
import io.circe._
import networthcalculator.domain.assets._
import networthcalculator.domain.users.UserId
import squants.market._

import java.time.{Month, Year}
import scala.util.control.NoStackTrace

object transactions {
  opaque type TransactionId = Long
  object TransactionId {
    def fromLong(d: Long): TransactionId = d

    given Decoder[TransactionId] = Decoder.decodeLong
    given Encoder[TransactionId] = Encoder.encodeLong
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
      month: Int,
      year: Year,
      transactions: List[TransactionValue]
  )

  final case class TransactionValue(amount: BigDecimal, currency: String, assetId: AssetId)

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

  object MoneyImplicits {
    given moneyRead(using fxContext: MoneyContext): Read[Money] =
      Read[(BigDecimal, String)].map { case (total, currency) =>
        Money(
          total,
          currency
        ).get
      }

    given Encoder[Money] = new Encoder[Money] {
      final def apply(money: Money): Json = Json.obj(
        ("amount", Json.fromBigDecimal(money.amount)),
        ("currency", Json.fromString(money.currency.code))
      )
    }
  }

  final case class TransactionAlreadyCreated(error: String) extends NoStackTrace

}
