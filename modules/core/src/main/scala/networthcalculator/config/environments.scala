package networthcalculator.config

import ciris.ConfigDecoder

object environments {

  enum AppEnvironment {
    case Test, Prod
  }
  object AppEnvironment {

    given appEnvConfigDecoder: ConfigDecoder[String, AppEnvironment] =
      ConfigDecoder[String, String].mapOption("AppEnv")(apply)

    private def apply(value: String): Option[AppEnvironment] =
      value.toLowerCase match {
        case "test" => Some(AppEnvironment.Test)
        case "prod" => Some(AppEnvironment.Prod)
        case _      => None
      }
  }
}
