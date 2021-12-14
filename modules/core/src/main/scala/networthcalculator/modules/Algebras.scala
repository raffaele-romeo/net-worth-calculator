package networthcalculator.modules

import cats.Parallel
import cats.effect.{Concurrent, Resource, Timer}
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import networthcalculator.algebras._
import networthcalculator.domain.users.{User, UserName}
import tsec.authentication.{AugmentedJWT, BackingStore, IdentityStore}
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256

object Algebras {

  def make[F[_]: Concurrent: Parallel: Timer](
      transactor: Resource[F, HikariTransactor[F]],
      getId: AugmentedJWT[HMACSHA256, UserName] => SecureRandomId,
      redis: RedisCommands[F, String, String]
  ): F[Algebras[F]] =
    for {
      users <- LiveUsers.make(transactor)
      tokens <- LiveTokens.make(getId, redis)
      crypto <- LiveEncrypter.make[F]
      assets <- LiveAssets.make[F](transactor)
      healthcheck <- LiveHealthCheck.make[F](transactor, redis)
    } yield Algebras[F](users, tokens, crypto, assets, healthcheck)
}

final case class Algebras[F[_]] private (
    users: IdentityStore[F, UserName, User] with Users[F],
    tokens: BackingStore[F, SecureRandomId, AugmentedJWT[HMACSHA256, UserName]],
    crypto: Encrypter,
    assets: Assets[F],
    healthCheck: HealthCheck[F]
)
