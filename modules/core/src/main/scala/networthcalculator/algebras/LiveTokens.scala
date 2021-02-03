package networthcalculator.algebras

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.users.UserName
import tsec.authentication.{AugmentedJWT, BackingStore}
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256

object LiveTokens {

  def make[F[_]: Sync](
      getId: AugmentedJWT[HMACSHA256, UserName] => SecureRandomId,
      tokenExpiration: TokenExpiration,
      redis: RedisCommands[F, SecureRandomId, AugmentedJWT[HMACSHA256, UserName]]
  ): F[LiveTokens[F]] = {
    Sync[F]
      .delay {
        new LiveTokens[F](getId, tokenExpiration, redis)
      }
  }
}

final class LiveTokens[F[_]: Sync] private (
    getId: AugmentedJWT[HMACSHA256, UserName] => SecureRandomId,
    tokenExpiration: TokenExpiration,
    redis: RedisCommands[F, SecureRandomId, AugmentedJWT[HMACSHA256, UserName]]
) extends BackingStore[F, SecureRandomId, AugmentedJWT[HMACSHA256, UserName]] {

  private val TokenExpiration = tokenExpiration.value

  override def put(elem: AugmentedJWT[HMACSHA256, UserName]): F[AugmentedJWT[HMACSHA256, UserName]] = {
    redis.setEx(getId(elem), elem, TokenExpiration)
    Sync[F].pure(elem)
  }

  override def update(v: AugmentedJWT[HMACSHA256, UserName]): F[AugmentedJWT[HMACSHA256, UserName]] = {
    redis.setEx(getId(v), v, TokenExpiration)
    Sync[F].pure(v)
  }

  override def delete(id: SecureRandomId): F[Unit] = {
    redis.del(id) *> Sync[F].unit
  }

  override def get(id: SecureRandomId): OptionT[F, AugmentedJWT[HMACSHA256, UserName]] = {
    OptionT(redis.get(id))
  }
}
