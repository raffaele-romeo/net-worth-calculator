package networthcalculator.algebras

import networthcalculator.domain.assets.AssetType
import networthcalculator.domain.transactions.{
  CreateTransaction,
  ExplodeCreateTransaction,
  ValidTransaction
}
import networthcalculator.domain.users.{ Password, UserName, ValidUser }

trait ValidationService[F[_]]:
  def validate(
    transactions: List[ExplodeCreateTransaction]
  ): F[List[ValidTransaction]]
  def validate(username: UserName, password: Password): F[ValidUser]
  def validate(assetType: String): F[AssetType]
