package networthcalculator.algebras

import networthcalculator.domain.users._

trait EncryptionService[F[_]] {
  def encrypt(password: Password, salt: Salt): F[EncryptedPassword]
  def generateRandomSalt(): F[Salt]
  def checkPassword(
      encryptedPassword: EncryptedPassword,
      password: Password,
      salt: Salt
  ): F[Boolean]
}
