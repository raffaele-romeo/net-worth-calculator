package networthcalculator

import cats.effect._
import cats.syntax.all._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{Logger, SelfAwareStructuredLogger}
import networthcalculator.modules.{HttpApi, Security, Services}
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    config.Loader[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        AppResources.make[IO](cfg).use { res =>
          for {
            services <- IO.delay(Services.make[IO](res.psql, res.redis))
            security <- IO.delay(
              Security
                .make[IO](res.psql, res.redis, cfg.tokenExpiration, cfg.jwtAdmin.adminToken, cfg.jwtAdmin.adminUser)
            )
            api <- IO.delay(new HttpApi[IO](services, security))
            _ <- BlazeServerBuilder[IO](ExecutionContext.global)
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
