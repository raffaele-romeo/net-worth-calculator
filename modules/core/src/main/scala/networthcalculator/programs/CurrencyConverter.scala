package networthcalculator.programs

import cats.effect.Concurrent
import cats.effect.implicits.parallelForGenSpawn
import cats.implicits._
import networthcalculator.domain.transactions.{TotalNetWorth, AggregatedTransactions}
import networthcalculator.http.clients.CurrencyConversionClient
import squants.market.{Currency, MoneyContext, defaultCurrencySet}

import java.time.{LocalDate, Month, Year}

final class CurrencyConverter[F[_]: Concurrent](
    currencyConversionClient: CurrencyConversionClient[F]
) {
  def convertTotalNetWorthToTargetCurrency(
      targetCurrency: Currency,
      totalNetWorthByCurrency: F[List[AggregatedTransactions]]
  ): F[List[AggregatedTransactions]] =
    for {
      totNetWorthByCurrency <- totalNetWorthByCurrency
      result <- totNetWorthByCurrency.parTraverse(convertToTargetCurrency(targetCurrency, _))
    } yield result

  private def convertToTargetCurrency(
      targetCurrency: Currency,
      totalNetWorthByCurrency: AggregatedTransactions
  ): F[AggregatedTransactions] = {
    val now      = java.time.LocalDate.now
    val yearNow  = now.getYear
    val monthNow = java.time.LocalDate.now.getMonth

    val dateFrom: LocalDate =
      if (
        totalNetWorthByCurrency.month == monthNow && totalNetWorthByCurrency.year.getValue == yearNow
      ) LocalDate.now
      else
        LocalDate.of(
          totalNetWorthByCurrency.year.getValue,
          totalNetWorthByCurrency.month,
          totalNetWorthByCurrency.month.length(false)
        )

    for {
      exchangeRates <- currencyConversionClient.latestRates(targetCurrency, dateFrom)
      moneyContext = MoneyContext(targetCurrency, defaultCurrencySet, exchangeRates)
      result = AggregatedTransactions(
        List(totalNetWorthByCurrency.total.reduce(_ + _).in(targetCurrency)(moneyContext)),
        totalNetWorthByCurrency.month,
        totalNetWorthByCurrency.year
      )
    } yield result
  }
}
