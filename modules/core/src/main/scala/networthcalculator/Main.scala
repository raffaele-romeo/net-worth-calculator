package networthcalculator

import cats.effect.*
import com.comcast.ip4s.*
import networthcalculator.modules.*
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp:

  given unsafeLogger: log4cats.SelfAwareStructuredLogger[IO] =
    Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    config.Loader[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        AppResources.make[IO](cfg).use { res =>
          val services = Services.make[IO](res.psql, res.redis)
          val security = Security
            .make[IO](
              res.psql,
              res.redis,
              cfg.tokenExpiration
            )
          val httpClients =
            HttpClients.make[IO](cfg.currencyConversionConfig, res.client)
          val programs = Programs.make[IO](httpClients)
          val httpApp  = HttpApi.make[IO](services, security, programs)

          EmberServerBuilder
            .default[IO]
            .withHost(
              Ipv4Address
                .fromString(cfg.httpServerConfig.host.toString)
                .getOrElse(ipv4"0.0.0.0")
            )
            .withPort(
              Port
                .fromInt(cfg.httpServerConfig.port.toInt)
                .getOrElse(port"9000")
            )
            .withHttpApp(httpApp)
            .build
            .useForever
            .as(ExitCode.Success)
        }
    }
