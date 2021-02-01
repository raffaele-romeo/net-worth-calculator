package networthcalculator.ext

import cats.Eq
import doobie.{Put, Read}
import io.estatico.newtype.Coercible

object CoercibleDoobieCodec {
  implicit def coerciblePut[R, N](implicit ev: Coercible[Put[R], Put[N]], R: Put[R]): Put[N] = ev(R)
  implicit def coercibleRead[R, N](implicit ev: Coercible[Read[R], Read[N]], R: Read[R]): Read[N] = ev(R)
  implicit def coercibleEq[R, N](implicit ev: Coercible[Eq[R], Eq[N]], R: Eq[R]): Eq[N] = ev(R)
}
