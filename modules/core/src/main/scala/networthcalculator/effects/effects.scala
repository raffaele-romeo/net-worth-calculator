package networthcalculator

import cats.{ApplicativeError, MonadError}

package object effects {
  type ApThrow[F[_]] = ApplicativeError[F, Throwable]

  object ApThrow {
    def apply[F[_]](using ev: ApplicativeError[F, Throwable]): ApThrow[F] = ev
  }

  type MonadThrow[F[_]] = MonadError[F, Throwable]

  object MonadThrow {
    def apply[F[_]](using ev: MonadError[F, Throwable]): MonadThrow[F] = ev
  }

}
