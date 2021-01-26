//package networthcalculator.algebras.utils
//
//import cats.data.OptionT
//import cats.effect.{IO, Sync}
//import cats.{Eq, MonadError}
//
//import tsec.authentication._
//import tsec.authorization._
//import tsec.cipher.symmetric.jca._
//import tsec.common.SecureRandomId
//import tsec.jws.mac.JWTMac
//import tsec.mac.jca.HMACSHA256
//import networthcalculator.algebras.utils.AuthHelpers.Role.{Administrator, Customer}
//
//import scala.collection.mutable
//
//object AuthHelpers {
//
//  def redisBackingStore[F[_], I, V](getId: V => I)(implicit F: Sync[F]) = new BackingStore[F, I, V] {
//    private val storageMap = mutable.HashMap.empty[I, V]
//
//    def put(elem: V): F[V] = {
//      val map = storageMap.put(getId(elem), elem)
//      if (map.isEmpty)
//        F.pure(elem)
//      else
//        F.raiseError(new IllegalArgumentException)
//    }
//
//    def get(id: I): OptionT[F, V] =
//      OptionT.fromOption[F](storageMap.get(id))
//
//    def update(v: V): F[V] = {
//      storageMap.update(getId(v), v)
//      F.pure(v)
//    }
//
//    def delete(id: I): F[Unit] =
//      storageMap.remove(id) match {
//        case Some(_) => F.unit
//        case None => F.raiseError(new IllegalArgumentException)
//      }
//  }
//
//}
