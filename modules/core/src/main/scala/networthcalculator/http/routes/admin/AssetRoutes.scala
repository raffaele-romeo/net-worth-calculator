package networthcalculator.http.routes.admin

import cats._
import cats.syntax.all._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import networthcalculator.algebras.AssetsService
import networthcalculator.domain.asset._
import networthcalculator.domain.users.AdminUser
import networthcalculator.effects.MonadThrow
import networthcalculator.http.decoder._
import networthcalculator.http.json._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final class AssetRoutes[F[_]: Defer: JsonDecoder: MonadThrow: SelfAwareStructuredLogger](
    assets: AssetsService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/assets"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case _ @GET -> Root asAuthed _ =>
      Ok(assets.findAll)

    case req @ POST -> Root as _ =>
      req.req.decodeR[CreateAsset] { asset =>
        assets.create(asset.assetType.toDomain) *> Created()
      }

    case req @ PUT -> Root as _ =>
      req.req.decodeR[UpdateAsset] { asset =>
        assets.update(asset.toDomain) *> Ok()
      }

    case DELETE -> Root / LongVar(id) as _ =>
      assets.delete(AssetId(id)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
