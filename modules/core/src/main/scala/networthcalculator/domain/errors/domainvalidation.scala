package networthcalculator.domain.errors

import scala.util.control.NoStackTrace

sealed trait DomainValidation {
  def errorMessage: String
}

case object UsernameHasSpecialCharacters extends DomainValidation {
  def errorMessage: String = "Username has to be a valid email."
}

case object PasswordDoesNotMeetCriteria extends DomainValidation {
  def errorMessage: String =
    "Password must be at least 10 characters long, including an uppercase and a lowercase letter, one number and one special character."
}

final case class DomainValidationErrors(errors: List[String]) extends NoStackTrace
