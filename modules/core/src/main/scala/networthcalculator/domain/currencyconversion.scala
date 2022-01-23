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

  final case class CurrencyConversion(currencies: List[Currency])

  object CurrencyConversion {
    given Decoder[CurrencyConversion] =
      Decoder[Map[String, BigDecimal]]
        .prepare(_.downField("data"))
        .map(kvs =>
          CurrencyConversion(
            kvs.map { case (k, v) =>
              Currency(CurrencyName(k), CurrencyValue(v))
            }.toList
          )
        )
  }

  final case class Currency(name: CurrencyName, value: CurrencyValue)

  final case class CurrencyConversionError(error: String) extends NoStackTrace
}
