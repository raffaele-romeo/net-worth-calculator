package networthcalculator.algebras

import networthcalculator.domain.account.{ Account, AccountId, AccountType }

trait Accounts[F[_]] {
  def findAll: F[List[Account]]
  def create(accountType: AccountType): F[Unit]
  def delete(accountId: AccountId): F[Unit]
  def update(account: Account): F[Unit]
}
