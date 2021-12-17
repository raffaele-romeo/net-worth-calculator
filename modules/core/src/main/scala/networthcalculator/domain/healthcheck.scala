package networthcalculator.domain

object healthcheck {
  final case class RedisStatus(value: Boolean)
  final case class PostgresStatus(value: Boolean)

  final case class AppStatus(
      redis: RedisStatus,
      postgres: PostgresStatus
  )
}
