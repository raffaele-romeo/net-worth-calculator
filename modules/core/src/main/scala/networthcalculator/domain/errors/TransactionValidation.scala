package networthcalculator.domain.errors

import scala.util.control.NoStackTrace

sealed trait TransactionValidation {
  def errorMessage: String
}

case class CurrencyIsNotSupported(errorMessage: String) extends TransactionValidation

case class MonthIsNotValid(errorMessage: String) extends TransactionValidation

final case class TransactionValidationErrors(errors: List[String]) extends NoStackTrace
