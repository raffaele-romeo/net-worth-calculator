package networthcalculator.http

import networthcalculator.domain.assets._
import org.http4s._
import org.http4s.dsl.impl._

import java.time.Year

object HttpParam {

  given QueryParamDecoder[Year]      = QueryParamDecoder[Int].map(Year.of)
  given QueryParamDecoder[AssetType] = QueryParamDecoder[String].map(AssetType.of)

  object OptionalYearQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Year]("year")

  object AssetQueryParamMatcher extends QueryParamDecoderMatcher[AssetType]("assetType")
}
