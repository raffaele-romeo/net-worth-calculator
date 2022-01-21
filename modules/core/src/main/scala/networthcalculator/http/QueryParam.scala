package networthcalculator.http

import org.http4s._
import org.http4s.dsl.impl._

import java.time.Year

object QueryParam {

  given QueryParamDecoder[Year] = QueryParamDecoder[Int].map(Year.of)

  object OptionalYearQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Year]("year")

  object AssetNameFlag extends FlagQueryParamMatcher("assetName")
}
