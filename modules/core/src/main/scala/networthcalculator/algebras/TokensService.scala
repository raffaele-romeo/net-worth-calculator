package networthcalculator.algebras

import networthcalculator.domain.users.UserName
import com.nimbusds.jose._
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.tokens.JwtToken

trait TokensService[F[_]] {
  def generateToken(
      userName: UserName,
      expiresIn: TokenExpiration,
      jwsAlgorithm: JWSAlgorithm
  ): F[JwtToken]
  def findUserNameBy(token: JwtToken): F[Option[UserName]]
  def findTokenBy(userName: UserName): F[Option[JwtToken]]
  def storeToken(userName: UserName, token: JwtToken, expiresIn: TokenExpiration): F[Unit]
  def deleteToken(userName: UserName, token: JwtToken): F[Unit]
}