package networthcalculator.modules

import cats.effect.Concurrent
import networthcalculator.programs.CurrencyExchangeRate
import retry.Sleep
import org.typelevel.log4cats.Logger

object Programs:
  def make[F[_]: Concurrent: Sleep: Logger](
    httpClients: HttpClients[F]
  ): Programs[F] =
    val currencyExchangeRate =
      CurrencyExchangeRate[F](httpClients.currencyExchangeRateClient)

    Programs(currencyExchangeRate)

final case class Programs[F[_]](
  currencyExchangeRate: CurrencyExchangeRate[F]
)
