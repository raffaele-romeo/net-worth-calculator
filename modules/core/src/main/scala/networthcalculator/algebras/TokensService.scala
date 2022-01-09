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
  def findUserBy(token: JwtToken): F[Option[CommonUser]]
  def findTokenBy(userName: UserName): F[Option[JwtToken]]
  def storeToken(user: CommonUser, token: JwtToken, expiresIn: TokenExpiration): F[Unit]
  def deleteToken(userName: UserName, token: JwtToken): F[Unit]
}
