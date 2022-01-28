package networthcalculator

import cats.effect.*
import networthcalculator.modules._
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {

  given unsafeLogger: log4cats.SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

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
          val httpClients = HttpClients.make[IO](cfg.currencyConversionConfig, res.client)
          val programs    = Programs.make[IO](httpClients)
          val httpApp     = HttpApi.make[IO](services, security, programs)

          BlazeServerBuilder[IO]
            .bindHttp(
              cfg.httpServerConfig.port.toInt,
              cfg.httpServerConfig.host.toString
            )
            .withHttpApp(httpApp)
            .serve
            .compile
            .drain
            .as(ExitCode.Success)
        }
    }

}
