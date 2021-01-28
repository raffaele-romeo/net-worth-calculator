package networthcalculator.ext

import io.estatico.newtype.Coercible
import doobie.util.{Get, Put}

object doobienewtype {
  implicit def coerciblePut[N, P](implicit ev: Coercible[Put[P], Put[N]], R: Put[P]): Put[N] = ev(R)
  implicit def coercibleGet[N, P](implicit ev: Coercible[Get[P], Get[N]], R: Get[P]): Get[N] = ev(R)
}
