package networthcalculator.domain.errors

import scala.util.control.NoStackTrace

sealed trait TransactionValidation {
  def errorMessage: String
}

object TransactionValidation {

  final case class CurrencyIsNotSupported(errorMessage: String) extends TransactionValidation

  final case class MonthIsNotValid(errorMessage: String) extends TransactionValidation
}

final case class TransactionValidationErrors(errors: List[String]) extends NoStackTrace
