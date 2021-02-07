package networthcalculator.algebras

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands
import networthcalculator.domain.users.UserName
import networthcalculator.effects.MonadThrow
import tsec.authentication.{AugmentedJWT, BackingStore}
import tsec.common.SecureRandomId
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import java.time.Instant

object LiveTokens {

  def make[F[_]: Sync](
      getId: AugmentedJWT[HMACSHA256, UserName] => SecureRandomId,
      redis: RedisCommands[F, String, String]
  ): F[LiveTokens[F]] = {
    Sync[F]
      .delay {
        new LiveTokens[F](getId, redis)
      }
  }
}

final class LiveTokens[F[_]: Sync: MonadThrow] private (
    getId: AugmentedJWT[HMACSHA256, UserName] => SecureRandomId,
    redis: RedisCommands[F, String, String]
) extends BackingStore[F, SecureRandomId, AugmentedJWT[HMACSHA256, UserName]] {

  override def put(elem: AugmentedJWT[HMACSHA256, UserName]): F[AugmentedJWT[HMACSHA256, UserName]] = {
    redis.hmSet(getId(elem), jwtToMap(elem))
    Sync[F].pure(elem)
  }

  override def update(v: AugmentedJWT[HMACSHA256, UserName]): F[AugmentedJWT[HMACSHA256, UserName]] = {
    redis.hmSet(getId(v), jwtToMap(v))
    Sync[F].pure(v)
  }

  override def delete(id: SecureRandomId): F[Unit] =
    redis.del(id) *> Sync[F].unit

  override def get(id: SecureRandomId): OptionT[F, AugmentedJWT[HMACSHA256, UserName]] = {
    OptionT(redis.hGetAll(id).flatMap(mapToJWT))
  }

  private def jwtToMap(jwt: AugmentedJWT[HMACSHA256, UserName]): Map[String, String] = {
    val informationToStore = Map(
      "expiry" -> jwt.expiry.toEpochMilli.toString,
      "id" -> jwt.id,
      "jwt" -> JWTMac.toEncodedString[F, HMACSHA256](jwt.jwt),
      "identity" -> jwt.identity.value
    )

    jwt.lastTouched match {
      case Some(value) => informationToStore.+(("lastTouched", value.toEpochMilli.toString))
      case None => informationToStore
    }
  }

  private def mapToJWT(storedInfo: Map[String, String]): F[Option[AugmentedJWT[HMACSHA256, UserName]]] = {
    if (storedInfo.isEmpty)
      Sync[F].pure(None)
    else
      JWTMac.parseUnverified[F, HMACSHA256](storedInfo("jwt")).map { mac =>
        AugmentedJWT[HMACSHA256, UserName](
          id = SecureRandomId.coerce(storedInfo("id")),
          identity = UserName(storedInfo("identity")),
          expiry = Instant.ofEpochMilli(storedInfo("expiry").toLong),
          jwt = mac,
          lastTouched = storedInfo.get("lastTouched").map(_.toLong).map(Instant.ofEpochMilli)
        ).some
      }
  }
}
