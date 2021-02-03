package networthcalculator.modules

import cats.Parallel
import cats.effect.{Concurrent, Resource, Timer}
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.algebras._
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.users.{User, UserName}
import tsec.authentication.{AugmentedJWT, BackingStore}
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256

object Algebras {

  def make[F[_]: Concurrent: Parallel: Timer](
      transactor: Resource[F, HikariTransactor[F]],
      getId: AugmentedJWT[HMACSHA256, UserName] => SecureRandomId,
      tokenExpiration: TokenExpiration,
      redis: RedisCommands[F, SecureRandomId, AugmentedJWT[HMACSHA256, UserName]]
  ): F[Algebras[F]] =
    for {
      users <- LiveUsers.make(transactor)
      tokens <- LiveTokens.make(getId, tokenExpiration, redis)
      crypto <- LiveCrypto.make[F]
      healthcheck <- LiveHealthCheck.make[F](transactor, redis)
    } yield Algebras[F](users, tokens, crypto, healthcheck)
}

final case class Algebras[F[_]] private (
    users: BackingStore[F, UserName, User],
    tokens: BackingStore[F, SecureRandomId, AugmentedJWT[HMACSHA256, UserName]],
    crypto: Crypto,
    healthCheck: HealthCheck[F]
)
