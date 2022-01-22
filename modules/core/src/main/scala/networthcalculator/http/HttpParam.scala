package networthcalculator.http

import cats.implicits.catsSyntaxEither
import networthcalculator.domain.assets._
import org.http4s._
import org.http4s.dsl.impl._

import java.time.Year
import scala.util.Try

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
}
