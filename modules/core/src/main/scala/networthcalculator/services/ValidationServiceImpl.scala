package networthcalculator.services

import cats.MonadThrow
import cats.data.Validated._
import cats.data.ValidatedNec
import cats.effect.Sync
import cats.implicits.*
import cats.kernel.Semigroup
import cats.syntax.all.*
import networthcalculator.algebras.ValidationService
import networthcalculator.domain.assets.{AssetType, AssetTypeNotAllowed}
import networthcalculator.domain.errors.AuthValidation.{
  PasswordDoesNotMeetCriteria,
  UsernameDoesNotMeetCriteria
}
import networthcalculator.domain.errors.TransactionValidation.{
  CurrencyIsNotSupported,
  MonthIsNotValid
}
import networthcalculator.domain.errors._
import networthcalculator.domain.transactions.*
import networthcalculator.domain.users.{Password, UserName, ValidUser}
import squants.market.{Money, defaultMoneyContext}

import java.time.Month
import scala.util.{Failure, Success, Try}

object ValidationServiceImpl {
  def make[F[_]](using S: Sync[F], ME: MonadThrow[F]): ValidationService[F] =
    new ValidationService[F] {
      override def validate(
          transactions: List[ExplodeCreateTransaction]
      ): F[List[ValidTransaction]] =
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
        ME.catchNonFatal(AssetType.of(assetType)).adaptError { case e =>
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

  def validateForm(
      transactions: List[ExplodeCreateTransaction]
  ): ValidationResult[List[ValidTransaction]] = {
    transactions.map(validateTransaction).reduce(_ combine _)
  }

  private def validateCurrency(amount: BigDecimal, currency: String): ValidationResult[Money] =
    Money(amount, currency)(defaultMoneyContext) match {
      case Failure(e) => CurrencyIsNotSupported(e.getMessage).invalidNec
      case Success(v) => v.valid
    }

  private def validateMonth(month: Int): ValidationResult[Month] = Try(Month.of(month)).fold(
    _ =>
      MonthIsNotValid(
        s"Month must be one of the following: ${Month.values.mkString(", ")}"
      ).invalidNec,
    value => value.valid
  )

  private def validateTransaction(
      transaction: ExplodeCreateTransaction
  ): ValidationResult[List[ValidTransaction]] = {
    (
      validateCurrency(transaction.amount, transaction.currency),
      validateMonth(transaction.month)
    ).mapN((money, month) =>
      List(ValidTransaction(money, month, transaction.year, transaction.assetId))
    )
  }
}
