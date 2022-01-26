package networthcalculator.http.clients

import cats.*
import cats.effect.*
import cats.implicits.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.parser.parse
import networthcalculator.config.data.CurrencyConversionConfig
import networthcalculator.domain.currencyconversion.{CurrencyConversion, CurrencyConversionError}
import org.http4s.Status.{NotFound, Successful}
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.syntax.all.*
import org.http4s.{Status, Uri}
import org.typelevel.log4cats.Logger
import squants.market._

import java.util.UUID

trait CurrencyConversionClient[F[_]] {
  def latestRates(
      baseCurrency: Currency,
      dateFrom: Option[String]
  ): F[List[CurrencyExchangeRate]]
}

object CurrencyConversionClient {
  def make[F[_]: JsonDecoder: Concurrent: Logger](
      currencyConversionConfig: CurrencyConversionConfig,
      client: Client[F]
  ): CurrencyConversionClient[F] = new CurrencyConversionClient[F]
    with Http4sClientDsl[F] {
    override def latestRates(
        baseCurrency: Currency,
        dateFrom: Option[String]
    ): F[List[CurrencyExchangeRate]] = {

      val uri = currencyConversionConfig.baseUri
        .withQueryParam("apikey", currencyConversionConfig.apiKey.toString)
        .withQueryParam("base_currency", baseCurrency.code)
        .withQueryParam("date_from", dateFrom.fold(java.time.LocalDate.now.toString)(identity))

      for {
        currencyConversion <- client.get(uri) {
          case Successful(resp) =>
            resp.decodeJson[CurrencyConversion]
          case resp =>
            CurrencyConversionError(
              resp.status.code,
              resp.status.reason
            ).raiseError[F, CurrencyConversion]
        }
      } yield createExchangeRates(baseCurrency, currencyConversion)
    }

    private def createExchangeRates(
        baseCurrency: Currency,
        currencyConversion: CurrencyConversion
    ): List[CurrencyExchangeRate] = {
      given MoneyContext = MoneyContext(baseCurrency, defaultCurrencySet, Nil)

      val availableCurrency =
        currencyConversion.currencies.filter(currency =>
          Currency(currency.name.toString).isSuccess & currency.name.toString != baseCurrency.code
        )

      availableCurrency.map { currency =>
        baseCurrency / (Currency(currency.name.toString).get)(currency.value.toBigDecimal)
      }
    }
  }
}
