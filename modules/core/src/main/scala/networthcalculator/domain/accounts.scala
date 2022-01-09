package networthcalculator.domain

import scala.util.control.NoStackTrace
import io.circe._
import users.UserId
import doobie.util._

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

  opaque type AccountType = String
  object AccountType {
    def apply(d: String): AccountType = d

    given Decoder[AccountType] = Decoder.decodeString
    given Encoder[AccountType] = Encoder.encodeString
  }

  final case class Account(
      accountId: AccountId,
      accountType: AccountType,
      accountName: AccountName,
      userId: UserId
  ) derives Encoder.AsObject

  object Account {
    given accountRead: Read[Account] =
      Read[(Long, String, String, Long)].map { case (id, accountType, accountName, userId) =>
        Account(
          AccountId(id),
          AccountType(accountType),
          AccountName(accountName),
          UserId(userId)
        )
      }
    given accountWrite: Write[Account] =
      Write[(Long, String, String, Long)].contramap { account =>
        (
          account.accountId.toLong,
          account.accountType.toString,
          account.accountName.toString,
          account.userId.toLong
        )
      }
  }

  final case class CreateAccount(accountType: AccountType, accountName: AccountName)

  final case class AccountTypeNotAllowed(accountType: AccountType) extends NoStackTrace
}
