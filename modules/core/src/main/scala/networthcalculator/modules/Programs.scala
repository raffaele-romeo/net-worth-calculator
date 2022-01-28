package networthcalculator.modules

import cats.effect.Concurrent
import networthcalculator.programs.CurrencyExchangeRate

object Programs {
  def make[F[_]: Concurrent](
      httpClients: HttpClients[F]
  ): Programs[F] = {
    val currencyExchangeRate = CurrencyExchangeRate[F](httpClients.currencyExchangeRateClient)

    Programs(currencyExchangeRate)
  }

}

final case class Programs[F[_]](
    currencyExchangeRate: CurrencyExchangeRate[F]
)
