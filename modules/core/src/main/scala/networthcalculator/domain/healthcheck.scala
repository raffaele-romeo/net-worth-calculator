package networthcalculator.domain

import io.estatico.newtype.macros.newtype

object healthcheck {
  @newtype case class RedisStatus(value: Boolean)
  @newtype case class PostgresStatus(value: Boolean)
  @newtype case class ElasticsearchStatus(value: Boolean)

  case class AppStatus(
      redis: RedisStatus,
      postgres: PostgresStatus,
      ElasticsearchStatus: ElasticsearchStatus
  )
}
