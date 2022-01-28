package networthcalculator.domain

import io.circe.*

object healthcheck:
  opaque type RedisStatus = Boolean
  object RedisStatus:
    def apply(d: Boolean): RedisStatus = d

    given Decoder[RedisStatus] = Decoder.decodeBoolean

  opaque type PostgresStatus = Boolean
  object PostgresStatus:
    def apply(d: Boolean): PostgresStatus = d

    given Decoder[PostgresStatus] = Decoder.decodeBoolean

  final case class AppStatus(
    redis: RedisStatus,
    postgres: PostgresStatus
  ) derives Encoder.AsObject
