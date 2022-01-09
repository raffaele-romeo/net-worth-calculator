package networthcalculator.http.routes.account

import cats.effect.kernel.Concurrent
import cats.syntax.all._
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

final class AccountRoutes[F[_]: Concurrent: Logger](
    accounts: AccountsService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/accounts"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case _ @GET -> Root as user =>
      accounts.findAll(user.userId).flatMap(accounts => Ok(accounts.asJson))

    case req @ POST -> Root as user =>
      req.req
        .decodeR[CreateAccount] { account =>
          accounts.create(account, user.userId) *> Created()
        }

    case DELETE -> Root / LongVar(id) as user =>
      accounts.delete(AccountId(id), user.userId) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
