package networthcalculator.services

import cats.effect.Sync
import networthcalculator.algebras.EncryptionService
import networthcalculator.domain.users.{EncryptedPassword, Password, Salt}
import org.apache.commons.codec.binary.Hex
import cats.implicits.*

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object EncryptionServiceImpl {
  private val random                    = new SecureRandom()
  private val Iteration: Int            = 65536
  private val KeyLength: Int            = 128
  private val SaltSize: Int             = 64
  private val HashAlgorithmName: String = "PBKDF2WithHmacSHA1"

  def make[F[_]](using S: Sync[F]): EncryptionService[F] = {
    new EncryptionService[F] {

      override def encrypt(password: Password, salt: Salt): F[EncryptedPassword] = {
        S.delay {
          val keySpec =
            new PBEKeySpec(
              password.toString.toCharArray,
              salt.toString.getBytes(),
              Iteration,
              KeyLength
            )

          val keyFactory     = SecretKeyFactory.getInstance(HashAlgorithmName)
          val securePassword = keyFactory.generateSecret(keySpec).getEncoded

          keySpec.clearPassword()

          EncryptedPassword(new String(Hex.encodeHex(securePassword)))
        }
      }

      override def checkPassword(
          encryptedPassword: EncryptedPassword,
          password: Password,
          salt: Salt
      ): F[Boolean] = {
        for {
          encrypted <- this.encrypt(password, salt)
        } yield encrypted == encryptedPassword
      }

      override def generateRandomSalt(): F[Salt] = {
        S.delay {
          val salt = new Array[Byte](SaltSize)

          random.nextBytes(salt)

          Salt(new String(Hex.encodeHex(salt)))
        }
      }
    }
  }
}
