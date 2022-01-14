package networthcalculator.http.routes.asset

import cats.effect.Async
import cats.syntax.all.*
import cats.implicits.*
import networthcalculator.algebras.AssetsService
import networthcalculator.domain.assets.*
import networthcalculator.domain.users.CommonUser
import networthcalculator.http.decoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.Logger
import org.http4s.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import networthcalculator.effects.*

final class AssetRoutes[F[_]: Async: Logger](
    assets: AssetsService[F]
) extends Http4sDsl[F] {

  import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
  private[routes] val prefixPath = "/assets"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case _ @GET -> Root as user =>
      assets.findAll(user.userId).flatMap(assets => Ok(assets.asJson))

    case req @ POST -> Root as user =>
      req.req
        .decodeR[CreateAsset] { asset =>
          for {
            assetType <- validateInput(asset.assetType)
            result <- assets.create(
              assetType,
              asset.assetName,
              user.userId
            ) *> Created()
          } yield result
        }
        .recoverWith { case AssetTypeNotAllowed(assetType) =>
          BadRequest(assetType.asJson)
        }

    case DELETE -> Root / LongVar(id) as user =>
      assets.delete(AssetId(id), user.userId) *> NoContent()
  }

  private def validateInput(assetType: String): F[AssetType] = {
    Async[F].delay(AssetType.make(assetType)).adaptError { case e =>
      AssetTypeNotAllowed(
        s"Asset type: $assetType is not supported. Choose one of ${AssetType.values.mkString(", ")}"
      )
    }
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
