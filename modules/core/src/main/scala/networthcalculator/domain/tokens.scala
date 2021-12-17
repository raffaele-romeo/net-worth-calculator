package networthcalculator.domain

import networthcalculator.domain.users.UserName

import scala.util.control.NoStackTrace

object tokens {
  final case class JwtToken(value: String)

  final case class JwtTokenNotFound(username: UserName) extends NoStackTrace
  final case class UserNotFound(username: UserName) extends NoStackTrace
  final object InvalidJWTToken extends NoStackTrace
}
