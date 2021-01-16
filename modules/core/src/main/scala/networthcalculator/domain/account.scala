package networthcalculator.domain

import io.estatico.newtype.macros.newtype

object account {
  @newtype case class AccountId(value: Long)
  @newtype case class AccountType(name: String)

  case class Account(accountId: AccountId, accountType: AccountType)
}
