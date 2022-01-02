package networthcalculator

import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import networthcalculator.modules.{HttpApi, Security, Services}
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats

object Main extends IOApp {

  implicit def unsafeLogger: log4cats.SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    config.Loader[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        AppResources.make[IO](cfg).use { res =>
          for {
            services <- IO.delay(Services.make[IO](res.psql, res.redis))
            security <- IO.delay(
              Security
                .make[IO](
                  res.psql,
                  res.redis,
                  cfg.tokenExpiration,
                  cfg.jwtAdmin.adminToken,
                  cfg.jwtAdmin.adminUser
                )
            )
            api <- IO.delay(new HttpApi[IO](services, security))
            _ <- BlazeServerBuilder[IO]
              .bindHttp(
                cfg.httpServerConfig.port,
                cfg.httpServerConfig.host
              )
              .withHttpApp(api.httpApp)
              .serve
              .compile
              .drain
          } yield ExitCode.Success
        }
    }

}
