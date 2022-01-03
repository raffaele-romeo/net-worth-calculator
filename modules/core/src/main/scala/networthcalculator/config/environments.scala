package networthcalculator.config

import ciris.ConfigDecoder

object environments {

  sealed abstract class AppEnvironment extends Product with Serializable

  object AppEnvironment {
    case object Test extends AppEnvironment
    case object Prod extends AppEnvironment

    implicit val appEnvConfigDecoder: ConfigDecoder[String, AppEnvironment] =
      ConfigDecoder[String, String].mapOption("AppEnv")(apply)

    private def apply(value: String): Option[AppEnvironment] =
      value.toLowerCase match {
        case "test" => Some(AppEnvironment.Test)
        case "prod" => Some(AppEnvironment.Prod)
        case _      => None
      }
  }
}
