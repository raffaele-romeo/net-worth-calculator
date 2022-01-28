package networthcalculator.domain

import doobie.util.{ Read, Write }
import io.circe.*
import io.circe.generic.semiauto.*
import networthcalculator.domain.assets.*
import networthcalculator.domain.users.UserId
import squants.market.*

import java.text.DecimalFormat
import java.time.{ Month, Year }
import scala.util.control.NoStackTrace

object transactions:
  opaque type TransactionId = Long
  object TransactionId:
    def apply(d: Long): TransactionId = d

    given Decoder[TransactionId] = Decoder.decodeLong
    given Encoder[TransactionId] = Encoder.encodeLong

  extension (x: TransactionId)
    def toLong: Long = x

  final case class Transaction(
    transactionId: TransactionId,
    money: Money,
    month: Month,
    year: Year,
    assetId: AssetId,
    userId: UserId
  )

  object Transaction:
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

  final case class CreateTransaction(
    month: Int,
    year: Year,
    transactions: List[TransactionValue]
  )

  final case class TransactionValue(
    amount: BigDecimal,
    currency: String,
    assetId: AssetId
  )

  final case class AggregatedTransactions(
    totals: List[Money],
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

  object AggregatedTransactions:
    given Read[AggregatedTransactions] =
      Read[(BigDecimal, String, Int, Int)].map {
        case (total, currency, month, year) =>
          AggregatedTransactions(
            List(
              Money(
                total,
                currency
              )(defaultMoneyContext).get
            ),
            Month.of(month),
            Year.of(year)
          )
      }

  object codecs:
    private val df = new DecimalFormat("#,###.00")
    given Encoder[Money] =
      Encoder[String].contramap(money =>
        s"${money.currency.code} ${df.format(money.amount)}"
      )
    given Encoder[Year]  = Encoder[Int].contramap(_.getValue)
    given Encoder[Month] = Encoder[Int].contramap(_.getValue)

  final case class TransactionAlreadyCreated(error: String) extends NoStackTrace
