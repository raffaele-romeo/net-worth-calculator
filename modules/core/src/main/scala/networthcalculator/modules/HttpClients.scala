package networthcalculator.modules

import cats.effect.Concurrent
import networthcalculator.config.data.CurrencyConversionConfig
import networthcalculator.http.clients.CurrencyExchangeRateClient
import org.http4s.circe.JsonDecoder
import org.http4s.client.Client
import org.typelevel.log4cats.Logger

object HttpClients {
  def make[F[_]: Concurrent: Logger](
      currencyConversionConfig: CurrencyConversionConfig,
      client: Client[F]
  ): HttpClients[F] = {
    val currencyExchangeRateClient = CurrencyExchangeRateClient
      .make[F](currencyConversionConfig, client)

    HttpClients(currencyExchangeRateClient)
  }

}

final case class HttpClients[F[_]](
    currencyExchangeRateClient: CurrencyExchangeRateClient[F]
)
