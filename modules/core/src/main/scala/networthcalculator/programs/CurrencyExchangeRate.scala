package networthcalculator.programs

import cats.effect.Concurrent
import cats.effect.implicits.parallelForGenSpawn
import cats.implicits.*
import networthcalculator.domain.transactions.AggregatedTransactions
import networthcalculator.http.clients.CurrencyExchangeRateClient
import squants.market.{ Currency, MoneyContext, defaultCurrencySet }
import squants.market.{ CurrencyExchangeRate => CurrencyExchangeRateS }
import networthcalculator.utils.Utils
import networthcalculator.domain.currencyconversion.CurrencyConversion
import retry.syntax.all._
import retry.RetryPolicy
import retry.RetryPolicies._
import scala.concurrent.duration._

import java.time.{ LocalDate, Month, Year }
import cats.Applicative
import networthcalculator.domain.currencyconversion.CurrencyConversionError
import org.http4s.Status
import retry.Sleep

final class CurrencyExchangeRate[F[_]: Concurrent: Sleep](
  currencyExchangeRateClient: CurrencyExchangeRateClient[F]
):
  import CurrencyExchangeRate._
  def convertToTargetCurrency(
    targetCurrency: Currency,
    aggregatedTransactions: List[AggregatedTransactions]
  ): F[List[AggregatedTransactions]] =

    val (transactionsToBeConverted, transactionAlreadyInTargetCurrency) =
      aggregatedTransactions
        .partition(_.totals.exists(_.currency != targetCurrency))

    for
      transactionsConverted <- transactionsToBeConverted.parTraverse(
        convertToTargetCurrencyInternal(targetCurrency, _)
      )
      result = Utils.sort(transactionAlreadyInTargetCurrency ++ transactionsConverted)
    yield result

  private def convertToTargetCurrencyInternal(
    targetCurrency: Currency,
    aggregatedTransactions: AggregatedTransactions
  ): F[AggregatedTransactions] =
    val dateFrom: LocalDate =
      if aggregatedTransactions.month == LocalDate.now.getMonth && aggregatedTransactions.year.getValue == LocalDate.now.getYear
      then LocalDate.now
      else
        LocalDate.of(
          aggregatedTransactions.year.getValue,
          aggregatedTransactions.month,
          aggregatedTransactions.month.length(false)
        )

    for
      // TODO Use Redis as an LRU cache to redue the number of API's call
      currencyConversion <- currencyExchangeRateClient.latestRates(
        targetCurrency,
        dateFrom
      ).retryingOnSomeErrors(
        isWorthRetrying = isCurrencyConversionError[F],
        policy = retryPolicy,
        onError = retry.noop[F, Throwable]
      )

      exchangeRates = createExchangeRates(targetCurrency, currencyConversion)

      moneyContext = MoneyContext(
        targetCurrency,
        defaultCurrencySet,
        exchangeRates
      )

      result = AggregatedTransactions(
        List(
          aggregatedTransactions.totals
            .reduce((moneyL, moneyR) => (moneyL + moneyR)(moneyContext))
            .in(targetCurrency)(moneyContext)
        ),
        aggregatedTransactions.month,
        aggregatedTransactions.year
      )
    yield result

  private def createExchangeRates(
      baseCurrency: Currency,
      currencyConversion: CurrencyConversion
    ): List[CurrencyExchangeRateS] =
      given MoneyContext = MoneyContext(baseCurrency, defaultCurrencySet, Nil)

      val availableCurrency =
        currencyConversion.currencies.filter(currency =>
          Currency(
            currency.name.toString
          ).isSuccess & currency.name.toString != baseCurrency.code
        )

      availableCurrency.map { currency =>
        baseCurrency / (Currency(currency.name.toString).get)(
          currency.value.toBigDecimal
        )
      }

object CurrencyExchangeRate:
  val NumberOfRetries = 4
  val DelayBetweenRetries = 10.milliseconds
    
  def retryPolicy[F[_]: Applicative] = limitRetries[F](NumberOfRetries) join exponentialBackoff[F](DelayBetweenRetries)
  def isCurrencyConversionError[F[_]](e: Throwable)(using A: Applicative[F]): F[Boolean] = e match {
    case CurrencyConversionError(code, _) if code != Status.TooManyRequests.code => A.pure(true)
    case _ => A.pure(false)
}
