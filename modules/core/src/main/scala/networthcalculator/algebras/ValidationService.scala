package networthcalculator.algebras

import networthcalculator.domain.assets.AssetType
import networthcalculator.domain.transactions.{CreateTransaction, ValidTransaction}
import networthcalculator.domain.users.{Password, UserName, ValidUser}

trait ValidationService[F[_]] {
  def validate(transaction: CreateTransaction): F[ValidTransaction]
  def validate(username: UserName, password: Password): F[ValidUser]
  def validate(assetType: String): F[AssetType]
}
