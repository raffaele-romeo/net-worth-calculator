package networthcalculator.http.clients

import networthcalculator.domain.currencyconversion.*

import java.util.UUID
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.client.dsl.Http4sClientDsl
import networthcalculator.config.data.CurrencyConversionConfig
import org.typelevel.log4cats.Logger
import cats.effect.*
import cats.implicits.*
import io.circe.generic.auto.*
import org.http4s.Status.NotFound
import org.http4s.Status.Successful
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.syntax.all.*
import cats.*
import io.circe.Json
import io.circe.parser.parse
import org.http4s.Uri
import squants.market.{Currency, CurrencyExchangeRate, MoneyContext, defaultMoneyContext}

trait CurrencyConversionClient[F[_]] {
  def latestRates(
      baseCurrency: String,
      dateFrom: Option[String]
  ): F[List[CurrencyExchangeRate]]
}

object CurrencyConversionClient {
  def make[F[_]: JsonDecoder: Concurrent: Logger](
      currencyConversionConfig: CurrencyConversionConfig,
      client: Client[F]
  )(using ME: MonadThrow[F]): CurrencyConversionClient = new CurrencyConversionClient[F]
    with Http4sClientDsl[F] {
    override def latestRates(
        baseCurrency: String,
        dateFrom: Option[String]
    ): F[List[CurrencyExchangeRate]] = {

      val baseUri = uri"https://freecurrencyapi.net/api/v2/latest"
      val withQuery = baseUri
        .withQueryParam("apikey", currencyConversionConfig.apiKey.toString)
        .withQueryParam("base_currency", baseCurrency)
        .withQueryParam("date_from", dateFrom.fold(java.time.LocalDate.now.toString)(identity))

      for {
        currencyConversion <- client.get(withQuery) {
          case Successful(resp) =>
            resp.decodeJson[CurrencyConversion]
          case resp =>
            CurrencyConversionError(
              resp.status.toString
            ).raiseError[F, CurrencyConversion]
        }
      } yield createExchangeRates(baseCurrency, currencyConversion)
    }

    private def createExchangeRates(
        baseCurrency: String,
        currencyConversion: CurrencyConversion
    ): List[CurrencyExchangeRate] = {
      given MoneyContext    = defaultMoneyContext
      val baseCurrencyMoney = Currency(baseCurrency)

      val availableCurrency =
        currencyConversion.currencies.filter(currency =>
          Currency(currency.name.toString).isSuccess & currency.name.toString != baseCurrency
        )

      availableCurrency.map { currency =>
        baseCurrencyMoney.get / (Currency(currency.name.toString).get)(currency.value.toBigDecimal)
      }
    }
  }
}
