package networthcalculator.http.routes

import cats._
import cats.syntax.all._
import networthcalculator.algebras.Assets
import networthcalculator.domain.asset.{AssetId, CreateAsset, UpdateAsset}
import networthcalculator.http.json._
import org.http4s.HttpRoutes
import org.http4s.circe.{JsonDecoder, _}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final class AssetRoutes[F[_]: Defer: JsonDecoder: Monad](
    assets: Assets[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/assets"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      Ok(assets.findAll)

    case req @ POST -> Root =>
      req.asJsonDecode[CreateAsset].flatMap { asset =>
        assets.create(asset.assetType.toDomain) *> Created()
      }

    case req @ PUT -> Root =>
      req.asJsonDecode[UpdateAsset].flatMap { asset =>
        assets.update(asset.toDomain) *> Ok()
      }

    case DELETE -> Root / LongVar(id) =>
      assets.delete(AssetId(id)) *> NoContent()
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
