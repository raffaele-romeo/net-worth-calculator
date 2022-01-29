package networthcalculator.programs

import cats.Applicative
import cats.effect.Concurrent
import cats.effect.implicits.parallelForGenSpawn
import cats.implicits.*
import networthcalculator.domain.currencyconversion.{
  CurrencyConversion,
  CurrencyConversionError
}
import networthcalculator.domain.transactions.AggregatedTransactions
import networthcalculator.http.clients.CurrencyExchangeRateClient
import networthcalculator.utils.Utils
import org.http4s.Status
import org.typelevel.log4cats.Logger
import retry.RetryDetails.{ GivingUp, WillDelayAndRetry }
import retry.RetryPolicies.*
import retry.syntax.all.*
import retry.{ RetryDetails, RetryPolicy, Sleep }
import squants.market.{ CurrencyExchangeRate as CurrencyExchangeRateS, * }

import java.time.{ LocalDate, Month, Year }
import scala.concurrent.duration.*

final class CurrencyExchangeRate[F[_]: Concurrent: Sleep: Logger](
  currencyExchangeRateClient: CurrencyExchangeRateClient[F]
):
  import CurrencyExchangeRate.*
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
      result = Utils.sort(
        transactionAlreadyInTargetCurrency ++ transactionsConverted
      )
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
      currencyConversion <- currencyExchangeRateClient
        .latestRates(
          targetCurrency,
          dateFrom
        )
        .retryingOnSomeErrors(
          isWorthRetrying = isCurrencyConversionError[F],
          policy = retryPolicy,
          onError = onError[F]
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
  val NumberOfRetries     = 4
  val DelayBetweenRetries = 10.milliseconds

  def retryPolicy[F[_]: Applicative] = limitRetries[F](
    NumberOfRetries
  ) join exponentialBackoff[F](DelayBetweenRetries)

  def isCurrencyConversionError[F[_]](
    e: Throwable
  )(using A: Applicative[F]): F[Boolean] = e match {
    case CurrencyConversionError(code, _)
        if code != Status.TooManyRequests.code =>
      A.pure(true)
    case _ => A.pure(false)
  }

  def onError[F[_]: Logger](e: Throwable, details: RetryDetails): F[Unit] =
    details match {
      case WillDelayAndRetry(_, retriesSoFar, _) =>
        Logger[F].error(
          s"Failed to process exchange rate with ${e.getMessage}. So far we have retried $retriesSoFar times."
        )
      case GivingUp(totalRetries, _) =>
        Logger[F].error(
          s"Giving up on exchange rate after $totalRetries retries."
        )
    }
