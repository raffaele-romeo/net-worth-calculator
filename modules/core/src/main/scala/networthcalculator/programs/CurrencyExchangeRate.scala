package networthcalculator.programs

import cats.effect.Concurrent
import cats.effect.implicits.parallelForGenSpawn
import cats.implicits._
import networthcalculator.domain.transactions.AggregatedTransactions
import networthcalculator.http.clients.CurrencyExchangeRateClient
import squants.market.{Currency, MoneyContext, defaultCurrencySet}

import java.time.{LocalDate, Month, Year}

final class CurrencyExchangeRate[F[_]: Concurrent](
    currencyExchangeRateClient: CurrencyExchangeRateClient[F]
) {
  def convertToTargetCurrency(
      targetCurrency: Currency,
      totalNetWorthByCurrency: List[AggregatedTransactions]
  ): F[List[AggregatedTransactions]] = {
    
    val (transactionsToBeConverted, transactionAlreadyInTargetCurrency) = totalNetWorthByCurrency
        .partition(_.totals.exists(_.currency != targetCurrency))

    for {
      transactionsConverted <- transactionsToBeConverted.parTraverse(
        convertToTargetCurrencySingleTransactions(targetCurrency, _)
      )
      result = (transactionAlreadyInTargetCurrency ++ transactionsConverted).sorted(
        Ordering
          .by(
            (
                (totalNetWorth: AggregatedTransactions) => (totalNetWorth.year, totalNetWorth.month)
            )
          )
          .reverse
      )
    } yield result
  }

  private def convertToTargetCurrencySingleTransactions(
      targetCurrency: Currency,
      totalNetWorthByCurrency: AggregatedTransactions
  ): F[AggregatedTransactions] = {
    val dateFrom: LocalDate =
      if (
        totalNetWorthByCurrency.month == LocalDate.now.getMonth && totalNetWorthByCurrency.year.getValue == LocalDate.now.getYear
      ) LocalDate.now
      else
        LocalDate.of(
          totalNetWorthByCurrency.year.getValue,
          totalNetWorthByCurrency.month,
          totalNetWorthByCurrency.month.length(false)
        )

    for {
      exchangeRates <- currencyExchangeRateClient.latestRates(targetCurrency, dateFrom)

      moneyContext = MoneyContext(targetCurrency, defaultCurrencySet, exchangeRates)

      result = AggregatedTransactions(
        List(totalNetWorthByCurrency.totals
          .reduce((moneyL, moneyR) => (moneyL + moneyR)(moneyContext)).in(targetCurrency)(moneyContext)),
        totalNetWorthByCurrency.month,
        totalNetWorthByCurrency.year
      )
    } yield result
  }
}
