package networthcalculator.http.clients

import cats.effect.*
import cats.implicits.*
import networthcalculator.config.data.CurrencyConversionConfig
import networthcalculator.domain.currencyconversion.{
  CurrencyConversion,
  CurrencyConversionError
}
import org.http4s.Status.Successful
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.typelevel.log4cats.Logger
import squants.market.Currency

import java.time.LocalDate

trait CurrencyExchangeRateClient[F[_]]:
  def latestRates(
    baseCurrency: Currency,
    date: LocalDate
  ): F[CurrencyConversion]

object CurrencyExchangeRateClient:
  def make[F[_]: JsonDecoder: Concurrent: Logger](
    currencyConversionConfig: CurrencyConversionConfig,
    client: Client[F]
  ): CurrencyExchangeRateClient[F] = new CurrencyExchangeRateClient[F]
    with Http4sClientDsl[F]:
    override def latestRates(
      baseCurrency: Currency,
      date: LocalDate
    ): F[CurrencyConversion] =

      val uri = currencyConversionConfig.baseUri
        .withQueryParam("apikey", currencyConversionConfig.apiKey.toString)
        .withQueryParam("base_currency", baseCurrency.code)
        .withQueryParam("date_from", date.toString)
        .withQueryParam("date_to", date.toString)

      for
        _ <- Logger[F].info(s"Retrieving latest rates for ${date.toString}")
        currencyConversion <- client.get(uri) {
          case Successful(resp) =>
            resp.decodeJson[CurrencyConversion]
          case resp =>
            CurrencyConversionError(
              resp.status.code,
              resp.status.reason
            ).raiseError[F, CurrencyConversion]
        }
      yield currencyConversion
