package networthcalculator.algebras

import cats.effect.Sync
import networthcalculator.domain.auth._

import java.security.SecureRandom
import java.util
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait Crypto {
  def encrypt(password: Password, salt: Salt): EncryptedPassword
  def generateRandomSalt(saltSize: Int): String
  def passwordVerification(encryptedPassword: EncryptedPassword, password: Password, salt: Salt): Boolean
}

object LiveCrypto {

  def make[F[_]: Sync]: F[Crypto] = {
    Sync[F]
      .delay {
        new LiveCrypto(CryptContext())
      }
  }
}

final class LiveCrypto private (cryptContext: CryptContext) extends Crypto {

  private val Rand = new SecureRandom()

  override def encrypt(password: Password, salt: Salt): EncryptedPassword = {

    val charsPassword = password.value.toCharArray
    val keySpec = new PBEKeySpec(charsPassword, salt.value.getBytes(), cryptContext.iteration, cryptContext.keyLength)

    val keyFactory = SecretKeyFactory.getInstance(cryptContext.hashAlgorithmName)
    val securePassword = keyFactory.generateSecret(keySpec).getEncoded
    clearPassword(charsPassword, keySpec)

    EncryptedPassword(Base64.getEncoder.encodeToString(securePassword))
  }

  override def passwordVerification(encryptedPassword: EncryptedPassword, password: Password, salt: Salt): Boolean = {
    val tmpEncrypted = this.encrypt(password, salt)

    tmpEncrypted.value == encryptedPassword.value
  }

  override def generateRandomSalt(saltSize: Int = cryptContext.saltSize): String = {
    val salt = new Array[Byte](saltSize)
    Rand.nextBytes(salt)

    Base64.getEncoder.encodeToString(salt)
  }

  private def clearPassword(passwords: Array[Char], keySpec: PBEKeySpec): Unit = {
    util.Arrays.fill(passwords, Character.MIN_VALUE)
    keySpec.clearPassword()
  }
}

final case class CryptContext(
    iteration: Int = 999999,
    keyLength: Int = 512,
    saltSize: Int = 512,
    hashAlgorithmName: String = "PBKDF2WithHmacSHA3"
)
