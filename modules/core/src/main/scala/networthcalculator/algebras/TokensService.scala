package networthcalculator.algebras

import com.nimbusds.jose._
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{CommonUser, UserName}

trait TokensService[F[_]] {
  def generateToken(
      userName: UserName,
      expiresIn: TokenExpiration,
      jwsAlgorithm: JWSAlgorithm
  ): F[JwtToken]
  def findUserNameBy(token: JwtToken): F[Option[CommonUser]]
  def findTokenBy(userName: UserName): F[Option[JwtToken]]
  def storeToken(userName: UserName, token: JwtToken, expiresIn: TokenExpiration): F[Unit]
  def deleteToken(userName: UserName, token: JwtToken): F[Unit]
}
