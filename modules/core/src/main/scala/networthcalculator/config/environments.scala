package networthcalculator.config

import enumeratum.{CirisEnum, Enum, EnumEntry}
import enumeratum.EnumEntry.Lowercase

object environments {

  sealed abstract class AppEnvironment extends EnumEntry with Lowercase

  object AppEnvironment extends Enum[AppEnvironment] with CirisEnum[AppEnvironment] {
    case object Test extends AppEnvironment
    case object Prod extends AppEnvironment

    val values = findValues
  }
}
