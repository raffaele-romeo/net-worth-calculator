package networthcalculator.algebras

import cats.data.OptionT
import cats.effect.Sync
import dev.profunktor.redis4cats.RedisCommands
import tsec.authentication.BackingStore

object LiveTokenStore {

  def make[F[_]: Sync, I, V](getId: V => I, redis: RedisCommands[F, String, String]): F[LiveTokenStore[F, I, V]] = {
    Sync[F]
      .delay {
        new LiveTokenStore[F, I, V](getId, redis)
      }
  }
}

object LiveUserStore {

  def make[F[_]: Sync, I, V](getId: V => I, users: Users[F]): F[LiveUserStore[F, I, V]] = {
    Sync[F]
      .delay {
        new LiveUserStore[F, I, V](getId, users)
      }
  }
}

final class LiveTokenStore[F[_], I, V] private (
    getId: V => I,
    redis: RedisCommands[F, String, String]
) extends BackingStore[F, I, V] {

  override def put(elem: V): F[V] = ???

  override def update(v: V): F[V] = ???

  override def delete(id: I): F[Unit] = ???

  override def get(id: I): OptionT[F, V] = ???
}

final class LiveUserStore[F[_], I, V] private (
    getId: V => I,
    users: Users[F]
) extends BackingStore[F, I, V] {

  override def put(elem: V): F[V] = ???

  override def update(v: V): F[V] = ???

  override def delete(id: I): F[Unit] = ???

  override def get(id: I): OptionT[F, V] = ???
}
