package networthcalculator.algebras

import networthcalculator.domain.users._

trait Encryption {
  def encrypt(password: Password, salt: Salt): EncryptedPassword
  def generateRandomSalt(): Salt
  def checkPassword(encryptedPassword: EncryptedPassword, password: Password, salt: Salt): Boolean
}