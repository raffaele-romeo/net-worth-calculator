package networthcalculator.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import scala.util.control.NoStackTrace

object auth {
  @newtype case class UserId(value: Long)
  @newtype case class UserName(value: String)
  @newtype case class Password(value: String)

  case class User(id: UserId, name: UserName)

  // --------- user registration -----------

  @newtype case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = UserName(value.value.toLowerCase())
  }
  case class CreateUser(username: UserNameParam)

  case class UserNameInUse(username: UserName) extends NoStackTrace
}
