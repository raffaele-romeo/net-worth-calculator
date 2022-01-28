package networthcalculator.http

import cats.MonadThrow
import cats.data.Validated.{ Invalid, Valid }
import cats.data.{ NonEmptyList, ValidatedNel }
import cats.implicits.{ catsSyntaxApplicativeId, catsSyntaxEither }
import networthcalculator.domain.assets.*
import org.http4s.*
import org.http4s.dsl.impl.*
import squants.market.{ Currency, defaultMoneyContext }

import java.time.Year
import scala.util.Try
import scala.util.control.NoStackTrace

object httpParam:

  object AssetTypeVar:
    def unapply(str: String): Option[AssetType] =
      if !str.isEmpty then Try(AssetType.of(str)).toOption
      else None

  given QueryParamDecoder[Year] = QueryParamDecoder[Int]
    .emap(i =>
      Try(Year.of(i)).toEither.leftMap(t =>
        ParseFailure(t.getMessage, t.getMessage)
      )
    )

  given QueryParamDecoder[Currency] = QueryParamDecoder[String]
    .emap(str =>
      Currency(str.toUpperCase)(defaultMoneyContext).toEither.leftMap(t =>
        ParseFailure(t.getMessage, t.getMessage)
      )
    )

  object OptionalYearQueryParamMatcher
      extends OptionalValidatingQueryParamDecoderMatcher[Year]("year")

  object OptionalCurrencyQueryParamMatcher
      extends OptionalValidatingQueryParamDecoderMatcher[Currency]("currency")

  final case class UnableParsingQueryParams(name: String):
    val message = s"Unable parsing argument $name"

  def liftQueryParams[F[_], A](
    queryParam: ValidatedNel[UnableParsingQueryParams, A]
  )(using ME: MonadThrow[F]): F[A] =
    queryParam match
      case Valid(param) =>
        param.pure[F]
      case Invalid(failures) =>
        ME.raiseError(
          QueryParamValidationErrors(failures.map(_.message).toList)
        )

  final case class QueryParamValidationErrors(errors: List[String])
      extends NoStackTrace
