package networthcalculator.config

import io.estatico.newtype.macros.newtype

import scala.concurrent.duration._

object data {
  @newtype case class TokenExpiration(value: FiniteDuration)
}
