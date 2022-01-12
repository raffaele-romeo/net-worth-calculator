package networthcalculator.algebras

import networthcalculator.domain.accounts._
import networthcalculator.domain.users.UserId

trait AccountsService[F[_]] {
  def findAll(userId: UserId): F[List[Account]]
  def create(accountType: AssetType, accountName: AccountName, userId: UserId): F[Unit]
  def delete(accountId: AccountId, userId: UserId): F[Unit]
}
