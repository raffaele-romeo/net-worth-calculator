package networthcalculator.services

import networthcalculator.algebras.EncryptionService
import networthcalculator.domain.users.{EncryptedPassword, Password, Salt}
import org.apache.commons.codec.binary.Hex

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

final class EncryptionServiceImpl extends EncryptionService {

  private val random                    = new SecureRandom()
  private val Iteration: Int            = 65536
  private val KeyLength: Int            = 128
  private val SaltSize: Int             = 64
  private val HashAlgorithmName: String = "PBKDF2WithHmacSHA1"

  override def encrypt(password: Password, salt: Salt): EncryptedPassword = {

    val keySpec =
      new PBEKeySpec(password.value.toCharArray, salt.value.getBytes(), Iteration, KeyLength)

    val keyFactory     = SecretKeyFactory.getInstance(HashAlgorithmName)
    val securePassword = keyFactory.generateSecret(keySpec).getEncoded

    keySpec.clearPassword()

    EncryptedPassword(new String(Hex.encodeHex(securePassword)))
  }

  override def checkPassword(
      encryptedPassword: EncryptedPassword,
      password: Password,
      salt: Salt
  ): Boolean = {
    val tmpEncrypted = this.encrypt(password, salt)

    tmpEncrypted.value == encryptedPassword.value
  }

  override def generateRandomSalt(): Salt = {

    val salt = new Array[Byte](SaltSize)

    random.nextBytes(salt)

    Salt(new String(Hex.encodeHex(salt)))
  }
}
