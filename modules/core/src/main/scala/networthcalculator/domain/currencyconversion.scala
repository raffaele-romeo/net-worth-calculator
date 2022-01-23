package networthcalculator.domain

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import java.util.UUID
import scala.util.control.NoStackTrace

object currencyconversion {
  opaque type CurrencyName = String

  object CurrencyName {
    def apply(d: String): CurrencyName = d

    given Decoder[CurrencyName] = Decoder.decodeString
    given Encoder[CurrencyName] = Encoder.encodeString
  }

  extension (x: CurrencyName) {
    def toString: String = x
  }

  opaque type CurrencyValue = BigDecimal

  object CurrencyValue {
    def apply(d: BigDecimal): CurrencyValue = d

    given Decoder[CurrencyValue] = Decoder.decodeBigDecimal
    given Encoder[CurrencyValue] = Encoder.encodeBigDecimal
  }

  extension (x: CurrencyValue) {
    def toBigDecimal: BigDecimal = x
  }

  final case class CurrencyConversionQuery(
      apiKey: Option[String],
      base_currency: Option[String],
      date_from: Option[String],
      date_to: Option[String],
      timestamp: Option[Long]
  )

  final case class CurrencyConversion(
      query: Option[CurrencyConversionQuery],
      data: List[(CurrencyName, CurrencyValue)]
  )

  final case class CurrencyConversionError(error: String) extends NoStackTrace
}
