package networthcalculator.services

import cats.effect.Sync
import cats.implicits._
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import dev.profunktor.redis4cats.RedisCommands
import networthcalculator.algebras.TokensService
import networthcalculator.config.data.TokenExpiration
import networthcalculator.domain.tokens.JwtToken
import networthcalculator.domain.users.{CommonUser, UserName}

import java.security.SecureRandom
import java.util.{Date, UUID}

final class TokensServiceImpl[F[_]](
    redis: RedisCommands[F, String, String]
)(implicit S: Sync[F])
    extends TokensService[F] {

  override def generateToken(
      userName: UserName,
      expiresIn: TokenExpiration,
      jwsAlgorithm: JWSAlgorithm
  ): F[JwtToken] = S.delay {
    val random       = new SecureRandom()
    val sharedSecret = new Array[Byte](32)
    random.nextBytes(sharedSecret)
    val now    = new Date()
    val signer = new MACSigner(sharedSecret)
    val claimsSet = new JWTClaimsSet.Builder()
      .subject(userName.value)
      .issuer("net-worth-calculator")
      .expirationTime(new Date(now.toInstant.toEpochMilli + expiresIn.value.toMillis))
      .notBeforeTime(now)
      .issueTime(now)
      .jwtID(UUID.randomUUID().toString)
      .build()
    val signedJWT = new SignedJWT(new JWSHeader(jwsAlgorithm), claimsSet)
    signedJWT.sign(signer)

    JwtToken(signedJWT.serialize())
  }

  override def findUserNameBy(token: JwtToken): F[Option[CommonUser]] = {
    for {
      maybeUser <- redis.get(token.value)
    } yield maybeUser.map(user => CommonUser(UserName(user)))
  }

  override def findTokenBy(userName: UserName): F[Option[JwtToken]] = {
    for {
      maybeToken <- redis.get(userName.value)
    } yield maybeToken.map(JwtToken.apply)
  }

  override def storeToken(
      userName: UserName,
      token: JwtToken,
      expiresIn: TokenExpiration
  ): F[Unit] = {
    redis.setEx(userName.value, token.value, expiresIn.value) *>
      redis.setEx(token.value, userName.value, expiresIn.value)
  }

  override def deleteToken(userName: UserName, token: JwtToken): F[Unit] = {
    redis.del(userName.value) *> redis.del(token.value).void
  }
}
