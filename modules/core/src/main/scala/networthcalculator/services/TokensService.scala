package networthcalculator.services

import cats.effect.{Clock, Sync}
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import dev.profunktor.redis4cats.RedisCommands
import networthcalculator.algebras.Tokens
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.UserName

import java.security.SecureRandom
import java.util.Date
import scala.concurrent.duration.{Duration, MILLISECONDS}

import cats.implicits._

final class TokensService[F[_]] (
    redis: RedisCommands[F, String, String]
)(implicit S: Sync[F], C: Clock[F])
    extends Tokens[F] {

  override def generateToken(
      userName: UserName,
      expirationTime: Long,
      jwsAlgorithm: JWSAlgorithm = JWSAlgorithm.HS256
  ): F[JwtToken] = {
    for {
      random <- S.delay(new SecureRandom())
      sharedSecret = new Array[Byte](32)
      _ <- S.delay(random.nextBytes(sharedSecret))
      now <- Clock[F].realTime(MILLISECONDS)
      signer = new MACSigner(sharedSecret)
      claimsSet = new JWTClaimsSet.Builder()
        .subject(userName.value)
        .issuer("net-worth-calculator")
        .expirationTime(new Date(expirationTime))
        .notBeforeTime(new Date(now))
        .build()
      signedJWT = new SignedJWT(new JWSHeader(jwsAlgorithm), claimsSet)
      _ <- S.delay(signedJWT.sign(signer))
      token <- S.delay(signedJWT.serialize())
    } yield JwtToken(token)
  }

  override def getUserNameBy(token: JwtToken): F[Option[UserName]] = {
    for {
      maybeUser <- redis.get(token.value)
    } yield maybeUser.map(UserName)
  }

  override def getTokenBy(userName: UserName): F[Option[JwtToken]] = {
    for {
      maybeToken <- redis.get(userName.value)
    } yield maybeToken.map(JwtToken)
  }

  override def storeToken(userName: UserName, token: JwtToken, expiresAt: Long): F[Unit] = {
    redis.setEx(userName.value, token.value, Duration(expiresAt, MILLISECONDS)) *>
      redis.setEx(token.value, userName.value, Duration(expiresAt, MILLISECONDS))
  }

  override def deleteToken(userName: UserName, token: JwtToken): F[Unit] = {
    redis.del(userName.value) *> redis.del(token.value).void
  }
}
