package networthcalculator.config

import ciris.ConfigDecoder

object environments {

  enum AppEnvironment {
    case Local, Test
  }
  object AppEnvironment {

    given appEnvConfigDecoder: ConfigDecoder[String, AppEnvironment] =
      ConfigDecoder[String, String].mapOption("AppEnv")(apply)

    private def apply(value: String): Option[AppEnvironment] =
      value.toLowerCase match {
        case "test"  => Some(AppEnvironment.Test)
        case "local" => Some(AppEnvironment.Local)
        case _       => None
      }
  }
}
