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

object TokensServiceImpl {
  def make[F[_]](
      redis: RedisCommands[F, String, String]
  )(using S: Sync[F]): TokensService[F] =
    new TokensService[F] {

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
          .subject(userName.toString)
          .issuer("net-worth-calculator")
          .expirationTime(
            new Date(now.toInstant.toEpochMilli + expiresIn.toFiniteDuration.toMillis)
          )
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
          maybeUser <- redis.get(token.toString)
        } yield maybeUser.map(user => CommonUser(UserName(user)))
      }

      override def findTokenBy(userName: UserName): F[Option[JwtToken]] = {
        for {
          maybeToken <- redis.get(userName.toString)
        } yield maybeToken.map(JwtToken.apply)
      }

      override def storeToken(
          userName: UserName,
          token: JwtToken,
          expiresIn: TokenExpiration
      ): F[Unit] = {
        redis.setEx(userName.toString, token.toString, expiresIn.toFiniteDuration) *>
          redis.setEx(token.toString, userName.toString, expiresIn.toFiniteDuration)
      }

      override def deleteToken(userName: UserName, token: JwtToken): F[Unit] = {
        redis.del(userName.toString) *> redis.del(token.toString).void
      }
    }
}
