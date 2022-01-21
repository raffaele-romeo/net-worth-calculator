package networthcalculator.http.routes.secured

import cats.MonadThrow
import cats.effect.Concurrent
import cats.implicits.*
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import networthcalculator.algebras.{TransactionsService, ValidationService}
import networthcalculator.domain.assets.*
import networthcalculator.domain.errors.TransactionValidation.*
import networthcalculator.domain.errors.TransactionValidationErrors
import networthcalculator.domain.transactions._
import networthcalculator.domain.users.CommonUser
import networthcalculator.http.QueryParam._
import networthcalculator.http.decoder.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger
import squants.market.{MoneyContext, defaultMoneyContext}

import java.time.{Month, Year}

final class TransactionRoutes[F[_]: Concurrent: Logger](
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
            result <- transactionService.create(
              user.userId,
              validTransaction
            ) *> Created()
          } yield result
        }
        .recoverWith {
          case TransactionValidationErrors(error) => BadRequest(error.asJson)
          case TransactionAlreadyCreated(error)   => BadRequest(error.asJson)
        }

    case req @ GET -> Root / "net-worth" / "total" :? OptionalYearQueryParamMatcher(
          maybeYear
        ) as user => {
      given MoneyContext = defaultMoneyContext
      import MoneyImplicits.given

      val year = maybeYear.fold(Year.now)(identity)

      transactionService
        .totalNetWorthByCurrency(user.userId, Year.now)
        .flatMap(total => Ok(total.asJson))
    }
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
