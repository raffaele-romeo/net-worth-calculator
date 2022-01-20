package networthcalculator.services

import cats.MonadThrow
import cats.data.ValidatedNec
import cats.data.Validated.{Invalid, Valid}
import cats.effect.Sync
import networthcalculator.algebras.ValidationService
import networthcalculator.domain.errors.{
  AuthValidation,
  AuthValidationErrors,
  TransactionValidation,
  TransactionValidationErrors
}
import networthcalculator.domain.transactions.*
import networthcalculator.domain.errors.TransactionValidation.{
  CurrencyIsNotSupported,
  MonthIsNotValid
}
import networthcalculator.domain.transactions.{CreateTransaction, Month, ValidTransaction}
import squants.market.{Money, defaultMoneyContext}
import cats.syntax.all.*

import scala.util.Try
import cats.implicits.*
import cats.MonadThrow
import networthcalculator.domain.assets.{AssetType, AssetTypeNotAllowed}
import networthcalculator.domain.errors.AuthValidation.{
  PasswordDoesNotMeetCriteria,
  UsernameDoesNotMeetCriteria
}
import networthcalculator.domain.users.{Password, UserName, ValidUser}

import scala.util.{Failure, Success, Try}

object ValidationServiceImpl {
  def make[F[_]](using S: Sync[F], ME: MonadThrow[F]): ValidationService[F] =
    new ValidationService[F] {
      override def validate(transactions: List[ExplodeCreateTransaction]): F[List[ValidTransaction]] =
        TransactionValidatorNec.validateForm(transactions) match {
          case Valid(transactions) =>
            transactions.pure[F]
          case Invalid(e) =>
            ME.raiseError(TransactionValidationErrors(e.toList.map(_.errorMessage)))
        }

      override def validate(username: UserName, password: Password): F[ValidUser] =
        UserAuthValidatorNec.validateForm(username, password) match {
          case Valid(user) =>
            user.pure[F]
          case Invalid(e) =>
            ME.raiseError(AuthValidationErrors(e.toList.map(_.errorMessage)))
        }

      override def validate(assetType: String): F[AssetType] = {
        ME.catchNonFatal(AssetType.make(assetType)).adaptError { case e =>
          AssetTypeNotAllowed(
            s"Asset type: $assetType is not supported. Choose one of ${AssetType.values.mkString(", ")}"
          )
        }
      }
    }
}

object UserAuthValidatorNec {

  type ValidationResult[A] = ValidatedNec[AuthValidation, A]

  private def validateUserName(userName: UserName): ValidationResult[UserName] =
    if (
      userName.toString.matches(
        "^(?=.{1,64}@)[\\p{L}0-9_-]+(\\.[\\p{L}0-9_-]+)*@[^-][\\p{L}0-9-]+(\\.[\\p{L}0-9-]+)*(\\.[\\p{L}]{2,})$"
      )
    ) userName.validNec
    else UsernameDoesNotMeetCriteria.invalidNec

  private def validatePassword(password: Password): ValidationResult[Password] =
    if (
      password.toString.matches(
        "(?=^.{10,}$)((?=.*\\d)|(?=.*\\W+))(?![.\\n])(?=.*[A-Z])(?=.*[a-z]).*$"
      )
    )
      password.validNec
    else PasswordDoesNotMeetCriteria.invalidNec

  def validateForm(
      username: UserName,
      password: Password
  ): ValidationResult[ValidUser] = {
    (
      validateUserName(username),
      validatePassword(password)
    ).mapN(ValidUser.apply)
  }

}

object TransactionValidatorNec {

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
      transaction: List[ExplodeCreateTransaction]
  ): ValidationResult[List[ValidTransaction]] = {
    transaction.traverse( transaction =>
      (
      validateCurrency(transaction.amount, transaction.currency),
      validateMonth(transaction.month)
    ).mapN((money, month) => ValidTransaction(money, month, transaction.year, transaction.assetId))
    )
  }
}
