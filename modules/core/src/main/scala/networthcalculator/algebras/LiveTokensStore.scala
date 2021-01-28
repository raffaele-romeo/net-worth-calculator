package networthcalculator.algebras

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands
import networthcalculator.config.data.TokenExpiration
import tsec.authentication.{AugmentedJWT, BackingStore}
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256

object LiveTokensStore {

  def make[F[_]: Sync](
      getId: AugmentedJWT[HMACSHA256, Int] => SecureRandomId,
      tokenExpiration: TokenExpiration,
      redis: RedisCommands[F, SecureRandomId, AugmentedJWT[HMACSHA256, Int]]
  ): F[LiveTokensStore[F]] = {
    Sync[F]
      .delay {
        new LiveTokensStore[F](getId, tokenExpiration, redis)
      }
  }
}

final class LiveTokensStore[F[_]: Sync] private (
    getId: AugmentedJWT[HMACSHA256, Int] => SecureRandomId,
    tokenExpiration: TokenExpiration,
    redis: RedisCommands[F, SecureRandomId, AugmentedJWT[HMACSHA256, Int]]
) extends BackingStore[F, SecureRandomId, AugmentedJWT[HMACSHA256, Int]] {

  private val TokenExpiration = tokenExpiration.value

  override def put(elem: AugmentedJWT[HMACSHA256, Int]): F[AugmentedJWT[HMACSHA256, Int]] = {
    redis.setEx(getId(elem), elem, TokenExpiration)
    Sync[F].pure(elem)
  }

  override def update(v: AugmentedJWT[HMACSHA256, Int]): F[AugmentedJWT[HMACSHA256, Int]] = {
    redis.setEx(getId(v), v, TokenExpiration)
    Sync[F].pure(v)
  }

  override def delete(id: SecureRandomId): F[Unit] = {
    redis.del(id) *> Sync[F].unit
  }

  override def get(id: SecureRandomId): OptionT[F, AugmentedJWT[HMACSHA256, Int]] = {
    OptionT(redis.get(id))
  }
}
