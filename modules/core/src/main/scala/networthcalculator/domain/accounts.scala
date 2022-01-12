package networthcalculator.domain

import scala.util.control.NoStackTrace
import io.circe._
import users.UserId
import doobie.util.{Read => DRead, Write => DWrite}
import io.circe.generic.semiauto._

object accounts {

  opaque type AccountId = Long
  object AccountId {
    def apply(d: Long): AccountId = d

    given Decoder[AccountId] = Decoder.decodeLong
    given Encoder[AccountId] = Encoder.encodeLong
  }
  extension (x: AccountId) {
    def toLong: Long = x
  }

  opaque type AccountName = String
  object AccountName {
    def apply(d: String): AccountName = d

    given Decoder[AccountName] = Decoder.decodeString
    given Encoder[AccountName] = Encoder.encodeString
  }

  final case class Account(
      accountId: AccountId,
      accountType: AssetType,
      accountName: AccountName,
      userId: UserId
  ) derives Encoder.AsObject

  object Account {
    given accountRead: DRead[Account] =
      DRead[(Long, String, String, Long)].map { case (id, accountType, accountName, userId) =>
        Account(
          AccountId(id),
          AssetType.make(accountType),
          AccountName(accountName),
          UserId(userId)
        )
      }
    given accountWrite: DWrite[Account] =
      DWrite[(Long, String, String, Long)].contramap { account =>
        (
          account.accountId.toLong,
          account.accountType.toString,
          account.accountName.toString,
          account.userId.toLong
        )
      }
  }

  final case class CreateAccount(accountType: String, accountName: AccountName)

  enum AssetType {
    case Loan, Cash, Investment, Property
  }

  object AssetType {
    def make(s: String): AssetType = {
      AssetType.valueOf(s.toLowerCase.capitalize)
    }
  }

  final case class AccountTypeNotAllowed(error: String) extends NoStackTrace
}
