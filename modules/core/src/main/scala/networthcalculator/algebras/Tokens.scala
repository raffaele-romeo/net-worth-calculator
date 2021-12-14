package networthcalculator.algebras

import networthcalculator.domain.users.UserName
import com.nimbusds.jose._
import networthcalculator.domain.tokens.JwtToken

trait Tokens[F[_]] {
  def generateToken(
      userName: UserName,
      expirationTime: Long,
      jwsAlgorithm: JWSAlgorithm
  ): F[JwtToken]
  def getUserNameBy(token: JwtToken): F[Option[UserName]]
  def getTokenBy(userName: UserName): F[Option[JwtToken]]
  def storeToken(userName: UserName, token: JwtToken, expiresAt: Long): F[Unit]
  def deleteToken(userName: UserName, token: JwtToken): F[Unit]
}