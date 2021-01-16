package networthcalculator.domain

import io.estatico.newtype.macros.newtype

object auth {
  @newtype case class UserId(value: Long)
  @newtype case class UserName(value: String)
  @newtype case class Password(value: String)

  case class User(id: UserId, name: UserName)
}
