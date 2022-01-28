package networthcalculator.domain

import networthcalculator.domain.users.UserName

import scala.util.control.NoStackTrace

object tokens:
  opaque type JwtToken = String

  object JwtToken:
    def apply(d: String): JwtToken = d

  extension (x: JwtToken)
    def toString: String = x

  final case class UserNotFound(username: UserName) extends NoStackTrace
