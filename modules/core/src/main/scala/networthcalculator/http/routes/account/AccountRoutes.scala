package networthcalculator.http.routes.account

import cats.effect.kernel.Async
import cats.syntax.all._
import cats.implicits._
import networthcalculator.algebras.AccountsService
import networthcalculator.domain.accounts._
import networthcalculator.domain.users.CommonUser
import networthcalculator.http.decoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.Logger
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import networthcalculator.effects._

final class AccountRoutes[F[_]: Logger](
    accounts: AccountsService[F]
)(using A: Async[F])
    extends Http4sDsl[F] {

  private[routes] val prefixPath = "/accounts"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case _ @GET -> Root as user =>
      accounts.findAll(user.userId).flatMap(accounts => Ok(accounts.asJson))

    case req @ POST -> Root as user =>
      req.req
        .decodeR[CreateAccount] { account =>
          for {
            assetType <- validateInput(account.accountType)
            result <- accounts.create(
              assetType,
              account.accountName,
              user.userId
            ) *> Created()
          } yield result
        }
        .recoverWith { case AccountTypeNotAllowed(assetType) =>
          BadRequest(assetType.asJson)
        }

    case DELETE -> Root / LongVar(id) as user =>
      accounts.delete(AccountId(id), user.userId) *> NoContent()
  }

  private def validateInput(assetType: String): F[AssetType] = {
    A.delay(AssetType.make(assetType)).adaptError { case e =>
      AccountTypeNotAllowed(
        s"Asset type: $assetType is not supported. Choose one of ${AssetType.values.mkString(", ")}"
      )
    }
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
