package networthcalculator.http.routes.secured

import cats.MonadThrow
import cats.data.{NonEmptyList, ValidatedNel}
import cats.effect.kernel.Async
import cats.implicits.*
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import networthcalculator.algebras.{TransactionsService, ValidationService}
import networthcalculator.domain.assets.*
import networthcalculator.domain.errors.TransactionValidation.*
import networthcalculator.domain.errors.TransactionValidationErrors
import networthcalculator.domain.transactions._
import networthcalculator.domain.transactions.codecs.given
import networthcalculator.domain.users.CommonUser
import networthcalculator.http.decoder.*
import networthcalculator.http.httpParam._
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.LongVar
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger
import squants.market.MoneyContext

import java.time.{Month, Year}

final class TransactionRoutes[F[_]: Async: Logger](
    transactionService: TransactionsService[F],
    validationService: ValidationService[F]
)(using ME: MonadThrow[F])
    extends Http4sDsl[F] {

  import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
  private[routes] val prefixPath = "/transactions"

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

          for {
            validTransaction <- validationService.validate(explodedTransactions)
            _ <- transactionService.create(
              user.userId,
              validTransaction
            )
            response <- Created()
          } yield response
        }
        .recoverWith {
          case TransactionValidationErrors(error) => BadRequest(error.asJson)
          case TransactionAlreadyCreated(error)   => BadRequest(error.asJson)
        }

    case DELETE -> Root / LongVar(id) as user =>
      transactionService.delete(user.userId, TransactionId(id)) *> NoContent()

    case _ @GET -> Root as user =>
      for {
        transactions <- transactionService.findAll(user.userId)
        response     <- Ok(transactions.asJson)
      } yield response

    case req @ GET -> Root / "net-worth" / "total" :? OptionalYearQueryParamMatcher(
          maybeYear
        ) as user => {

      (for {
        maybeYear <- liftQueryParams(toUnableParsingQueryParam("year", maybeYear.sequence))
        totalNetWorth <- transactionService
          .totalNetWorthByCurrency(user.userId, maybeYear)
        response <- Ok(totalNetWorth.asJson)
      } yield response).recoverWith { case QueryParamValidationErrors(errors) =>
        BadRequest(errors.asJson)
      }
    }

    case req @ GET -> Root / "net-worth" / LongVar(assetId) :? OptionalYearQueryParamMatcher(
          maybeYear
        ) as user => {

      (for {
        maybeYear <- liftQueryParams(toUnableParsingQueryParam("year", maybeYear.sequence))
        totalNetWorth <- transactionService
          .netWorthByCurrencyAndAsset(user.userId, AssetId(assetId), maybeYear)
        response <- Ok(totalNetWorth.asJson)
      } yield response).recoverWith { case QueryParamValidationErrors(errors) =>
        BadRequest(errors.asJson)
      }

    }

    case req @ GET -> Root / "net-worth" :? OptionalYearQueryParamMatcher(
          maybeYear
        ) :? AssetQueryParamMatcher(assetTypeValidated) as user => {

      val validatedQueryParams =
        (
          toUnableParsingQueryParam("year", maybeYear.sequence),
          toUnableParsingQueryParam("assetType", assetTypeValidated)
        ).mapN((year, assetType) => (year, assetType))

      (for {
        params <- liftQueryParams(validatedQueryParams)
        totalNetWorth <- transactionService
          .netWorthByCurrencyAndAssetType(user.userId, params._2, params._1)
        response <- Ok(totalNetWorth.asJson)
      } yield response)
        .recoverWith { case QueryParamValidationErrors(errors) =>
          BadRequest(errors.asJson)
        }
    }
  }

  private def toUnableParsingQueryParam[A](
      name: String,
      queryParam: ValidatedNel[ParseFailure, A]
  ) =
    queryParam.leftMap(_ => NonEmptyList.one(UnableParsingQueryParams(name)))

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
