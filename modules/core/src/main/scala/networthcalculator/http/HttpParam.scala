package networthcalculator.http

import cats.MonadThrow
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, ValidatedNel}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEither}
import networthcalculator.domain.assets._
import org.http4s._
import org.http4s.dsl.impl._

import java.time.Year
import scala.util.Try
import scala.util.control.NoStackTrace

object httpParam {

  given QueryParamDecoder[AssetType] = QueryParamDecoder[String]
    .emap(str =>
      Try(AssetType.of(str)).toEither.leftMap(t => ParseFailure(t.getMessage, t.getMessage))
    )

  given QueryParamDecoder[Year] = QueryParamDecoder[Int]
    .emap(i => Try(Year.of(i)).toEither.leftMap(t => ParseFailure(t.getMessage, t.getMessage)))

  object OptionalYearQueryParamMatcher
      extends OptionalValidatingQueryParamDecoderMatcher[Year]("year")
  object AssetQueryParamMatcher extends ValidatingQueryParamDecoderMatcher[AssetType]("assetType")

  final case class UnableParsingQueryParams(name: String) {
    val message = s"Unable parsing argument $name"
  }

  def liftQueryParams[F[_], A](
      queryParam: ValidatedNel[UnableParsingQueryParams, A]
  )(using ME: MonadThrow[F]): F[A] = {
    queryParam match {
      case Valid(param) =>
        param.pure[F]
      case Invalid(failures) =>
        ME.raiseError(QueryParamValidationErrors(failures.map(_.message).toList))
    }
  }

  final case class QueryParamValidationErrors(errors: List[String]) extends NoStackTrace

}
