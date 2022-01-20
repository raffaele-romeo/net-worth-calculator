package networthcalculator.programs

import networthcalculator.domain.transactions._
import cats.data.{Validated, ValidatedNec}
import cats.data.Validated.{Invalid, Valid}
import cats.effect.Sync
import networthcalculator.domain.errors.TransactionValidation._
import networthcalculator.domain.errors.{TransactionValidation, TransactionValidationErrors}
import squants.market.Money
import squants.market.defaultMoneyContext
import scala.util.Failure
import scala.util.Success
import cats.syntax.all._
import scala.util.Try
import cats.implicits._
import cats.MonadThrow

trait TransactionProgram[F[_]] {
  def validateInput(transaction: CreateTransaction): F[ValidTransaction]
}

object TransactionProgramImpl {
  def make[F[_]](using S: Sync[F], ME: MonadThrow[F]): TransactionProgram[F] =
    new TransactionProgram[F] {
      override def validateInput(transaction: CreateTransaction): F[ValidTransaction] =
        FormValidatorNec.validateForm(transaction) match {
          case Valid(transaction) =>
            transaction.pure[F]
          case Invalid(e) =>
            ME.raiseError(TransactionValidationErrors(e.toList.map(_.errorMessage)))
        }
    }
}

object FormValidatorNec {

  type ValidationResult[A] = ValidatedNec[TransactionValidation, A]

  private def validateCurrency(amount: BigDecimal, currency: String): ValidationResult[Money] =
    Money(amount, currency)(defaultMoneyContext) match {
      case Failure(e) => CurrencyIsNotSupported(e.getMessage).invalidNec
      case Success(v) => v.valid
    }

  private def validateMonth(month: String): ValidationResult[Month] =
    Try(Month.fromString(month)) match {
      case Failure(e) =>
        MonthIsNotValid(
          s"Month must be one of the following: ${Month.values.mkString(", ")}"
        ).invalidNec
      case Success(v) => v.valid
    }

  def validateForm(
      transaction: CreateTransaction
  ): ValidationResult[ValidTransaction] = {
    (
      validateCurrency(transaction.amount, transaction.currency),
      validateMonth(transaction.month)
    ).mapN((money, month) => ValidTransaction(money, month, transaction.year, transaction.assetId))
  }

}
