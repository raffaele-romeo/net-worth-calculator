package networthcalculator.http.routes.admin

import cats.effect.kernel.Concurrent
import cats.syntax.all._
import networthcalculator.algebras.AssetsService
import networthcalculator.domain.asset._
import networthcalculator.domain.users.AdminUser
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

final class AssetRoutes[F[_]: Concurrent: Logger](
    assets: AssetsService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/assets"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case _ @GET -> Root as _ =>
      assets.findAll.flatMap(assets => Ok(assets.asJson))

    case req @ POST -> Root as _ =>
      req.req
        .decodeR[CreateAsset] { asset =>
          assets.create(asset.toDomain) *> Created()
        }
        .recoverWith { case AssetTypeInUse(assetType) =>
          Conflict(assetType.name)
        }

    case req @ PUT -> Root as _ =>
      req.req
        .decodeR[UpdateAsset] { asset =>
          assets.update(asset.toDomain) *> Ok()
        }
        .recoverWith { case AssetTypeInUse(assetType) =>
          Conflict(assetType.name)
        }

    case DELETE -> Root / LongVar(id) as _ =>
      assets.delete(AssetId(id)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
