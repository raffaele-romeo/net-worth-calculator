package networthcalculator

import cats.effect._
import cats.syntax.all._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{Logger, SelfAwareStructuredLogger}
import networthcalculator.modules.{Algebras, HttpApi}
import org.http4s.server.blaze.BlazeServerBuilder
import tsec.common.SecureRandomId

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    config.Loader[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        AppResources.make[IO](cfg).use { res =>
          for {
            algebras <- Algebras.make[IO](res.psql, s => SecureRandomId.coerce(s.id), res.redis)
            api <- HttpApi.make[IO](algebras, cfg.tokenExpiration)
            _ <- BlazeServerBuilder[IO](ExecutionContext.global)
              .bindHttp(
                cfg.httpServerConfig.port.value,
                cfg.httpServerConfig.host.value
              )
              .withHttpApp(api.httpApp)
              .serve
              .compile
              .drain
          } yield ExitCode.Success
        }
    }

}
