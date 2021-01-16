package networthcalculator.algebras

import networthcalculator.domain.healthcheck.AppStatus

trait HealthCheck[F[_]] {
  def status: F[AppStatus]
}
