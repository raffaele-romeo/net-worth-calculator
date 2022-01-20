package networthcalculator.http.routes.secured

import cats.effect.Concurrent
import cats.syntax.all.*
import cats.implicits.*
import networthcalculator.algebras.{TransactionsService, ValidationService}
import networthcalculator.domain.assets.*
import networthcalculator.domain.users.CommonUser
import networthcalculator.http.decoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.Logger
import org.http4s.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import cats.MonadThrow
import networthcalculator.domain.transactions.{ExplodeCreateTransaction, CreateTransaction}
import networthcalculator.domain.errors.TransactionValidation.*
import networthcalculator.domain.errors.TransactionValidationErrors
import networthcalculator.domain.transactions.TransactionAlreadyCreated

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
          val explodedTransactions = transaction.transactionValue.map(value => 
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
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
