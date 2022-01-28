package networthcalculator.http.routes.secured

import cats.data.{ NonEmptyList, ValidatedNel }
import cats.effect.Concurrent
import cats.implicits.*
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import networthcalculator.algebras.{ TransactionsService, ValidationService }
import networthcalculator.domain.assets.*
import networthcalculator.domain.currencyconversion.CurrencyConversionError
import networthcalculator.domain.errors.TransactionValidation.*
import networthcalculator.domain.errors.TransactionValidationErrors
import networthcalculator.domain.transactions.*
import networthcalculator.domain.transactions.codecs.given
import networthcalculator.domain.users.CommonUser
import networthcalculator.http.decoder.*
import networthcalculator.http.httpParam.*
import networthcalculator.programs.CurrencyExchangeRate
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.LongVar
import org.http4s.server.{ AuthMiddleware, Router }
import org.typelevel.log4cats.Logger
import squants.market.{ Currency, MoneyContext }

import java.time.{ Month, Year }

final class TransactionRoutes[F[_]](
  transactionService: TransactionsService[F],
  validationService: ValidationService[F],
  currencyExchangeRate: CurrencyExchangeRate[F]
)(using C: Concurrent[F], L: Logger[F])
    extends Http4sDsl[F]:

  import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
  private[routes] val prefixPathTransactions = "/transactions"
  private[routes] val prefixPathAssets       = "/assets"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case req @ POST -> Root as user =>
      req.req
        .decodeR[CreateTransaction] { transaction =>
          val explodedTransactions = transaction.transactions.map(value =>
            ExplodeCreateTransaction(
              value.amount,
              value.currency,
              transaction.month,
              transaction.year,
              value.assetId
            )
          )

          for
            validTransaction <- validationService.validate(explodedTransactions)
            _ <- transactionService.create(
              user.userId,
              validTransaction
            )
            response <- Created()
          yield response
        }
        .recoverWith {
          case TransactionValidationErrors(error) => BadRequest(error.asJson)
          case TransactionAlreadyCreated(error)   => BadRequest(error.asJson)
        }

    case DELETE -> Root / LongVar(id) as user =>
      transactionService.delete(user.userId, TransactionId(id)) *> NoContent()

    case _ @GET -> Root as user =>
      for
        transactions <- transactionService.findAll(user.userId)
        response     <- Ok(transactions.asJson)
      yield response

    case req @ GET -> Root / "net-worth" :? OptionalYearQueryParamMatcher(
          maybeYear
        ) :? OptionalCurrencyQueryParamMatcher(maybeCurrency) as user => {

      (for
        params <- liftQueryParams(
          validatedQueryParams(maybeYear.sequence, maybeCurrency.sequence)
        )
        (maybeYear, maybeCurrency) = params
        totalNetWorth <- transactionService.totalNetWorthByCurrency(
          user.userId,
          maybeYear
        )
        result <- maybeCurrency.fold(C.pure(totalNetWorth))(
          currencyExchangeRate.convertToTargetCurrency(_, totalNetWorth)
        )
        response <- Ok(result)
      yield response)
        .recoverWith {
          case QueryParamValidationErrors(errors) =>
            BadRequest(errors.asJson)
          case CurrencyConversionError(_, reason) =>
            BadRequest(reason.asJson)
        }
    }
  }

  private val httpRoutesAssets: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case req @ GET -> Root / LongVar(
          assetId
        ) / "transactions" :? OptionalYearQueryParamMatcher(
          maybeYear
        ) :? OptionalCurrencyQueryParamMatcher(maybeCurrency) as user => {

      (for
        params <- liftQueryParams(
          validatedQueryParams(maybeYear.sequence, maybeCurrency.sequence)
        )
        (maybeYear, maybeCurrency) = params
        totalNetWorth <- transactionService.findTransactionsByAssetId(
          user.userId,
          AssetId(assetId),
          maybeYear
        )
        result <- maybeCurrency.fold(C.pure(totalNetWorth))(
          currencyExchangeRate.convertToTargetCurrency(_, totalNetWorth)
        )
        response <- Ok(result.asJson)
      yield response)
        .recoverWith { case QueryParamValidationErrors(errors) =>
          BadRequest(errors.asJson)
        }

    }

    case req @ GET -> Root / AssetTypeVar(
          assetType
        ) / "transactions" :? OptionalYearQueryParamMatcher(
          maybeYear
        ) :? OptionalCurrencyQueryParamMatcher(maybeCurrency) as user => {

      (for
        params <- liftQueryParams(
          validatedQueryParams(maybeYear.sequence, maybeCurrency.sequence)
        )
        (maybeYear, maybeCurrency) = params
        totalNetWorth <- transactionService.findTransactionsByAssetType(
          user.userId,
          assetType,
          maybeYear
        )
        result <- maybeCurrency.fold(C.pure(totalNetWorth))(
          currencyExchangeRate.convertToTargetCurrency(_, totalNetWorth)
        )
        response <- Ok(result.asJson)
      yield response)
        .recoverWith { case QueryParamValidationErrors(errors) =>
          BadRequest(errors.asJson)
        }
    }
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(
      prefixPathTransactions -> authMiddleware(httpRoutes),
      prefixPathAssets       -> authMiddleware(httpRoutesAssets)
    )

  private def validatedQueryParams(
    maybeYear: ValidatedNel[ParseFailure, Option[Year]],
    maybeCurrency: ValidatedNel[ParseFailure, Option[Currency]]
  ) =
    (
      toUnableParsingQueryParam("year", maybeYear),
      toUnableParsingQueryParam("currency", maybeCurrency)
    ).mapN((year, currency) => (year, currency))

  private def toUnableParsingQueryParam[A](
    name: String,
    queryParam: ValidatedNel[ParseFailure, A]
  ) = queryParam.leftMap(_ => NonEmptyList.one(UnableParsingQueryParams(name)))
