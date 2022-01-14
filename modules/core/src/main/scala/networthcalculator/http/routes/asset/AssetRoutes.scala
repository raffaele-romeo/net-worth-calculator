package networthcalculator.http.routes.asset

import cats.effect.Concurrent
import cats.syntax.all._
import cats.implicits._
import networthcalculator.algebras.AssetsService
import networthcalculator.domain.assets._
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

final class AssetRoutes[F[_]: Concurrent: Logger](
    assets: AssetsService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/assets"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case _ @GET -> Root as user =>
      assets.findAll(user.userId).flatMap(assets => Ok(assets.asJson))

    case req @ POST -> Root as user =>
      req.req
        .decodeR[CreateAsset] { asset =>
          for {
            // assetType <- validateInput(account.accountType)
            result <- assets.create(
              AssetType.make(asset.assetType),
              asset.assetName,
              user.userId
            ) *> Created()
          } yield result
        }
//        .recoverWith { case AccountTypeNotAllowed(assetType) =>
//          BadRequest(assetType.asJson)
//        }

    case DELETE -> Root / LongVar(id) as user =>
      assets.delete(AssetId(id), user.userId) *> NoContent()
  }

//  private def validateInput(assetType: String): F[AssetType] = {
//    A.delay(AssetType.make(assetType)).adaptError { case e =>
//      AccountTypeNotAllowed(
//        s"Asset type: $assetType is not supported. Choose one of ${AssetType.values.mkString(", ")}"
//      )
//    }
//  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
