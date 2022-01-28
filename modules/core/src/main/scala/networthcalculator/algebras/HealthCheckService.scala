package networthcalculator.algebras

import networthcalculator.domain.healthcheck.AppStatus

trait HealthCheckService[F[_]]:
  def status: F[AppStatus]
