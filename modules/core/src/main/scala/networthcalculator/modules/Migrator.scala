package networthcalculator.modules

import cats.effect.Sync
import cats.syntax.all.*
import networthcalculator.config.data.PostgreSQLConfig
import org.flywaydb.core.Flyway
import org.typelevel.log4cats.Logger

object Migrator:

  def migrate[F[_]: Sync: Logger](cfg: PostgreSQLConfig): F[Unit] =
    Sync[F]
      .blocking {
        Flyway
          .configure()
          .dataSource(
            s"jdbc:postgresql://${cfg.host.toString}:${cfg.port.toInt}/${cfg.database.toString}",
            cfg.user.toString,
            cfg.password.toString
          )
          .locations("classpath:db/migration")
          .load()
          .migrate()
      }
      .flatMap { result =>
        Logger[F].info(
          s"Flyway: applied ${result.migrationsExecuted} migrations (schema version: ${result.targetSchemaVersion})"
        )
      }
