package networthcalculator.http.routes.admin

import cats._
import cats.syntax.all._
import networthcalculator.algebras.Assets
import networthcalculator.domain.asset._
import networthcalculator.domain.auth._
import networthcalculator.domain.users.{User, UserName}
import networthcalculator.effects.MonadThrow
import networthcalculator.http.decoder._
import networthcalculator.http.json._
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import tsec.authentication.{AugmentedJWT, SecuredRequestHandler, TSecAuthService, asAuthed}
import tsec.mac.jca.HMACSHA256

final class AssetRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    assets: Assets[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/assets"

  private val httpRoutes: TSecAuthService[User, AugmentedJWT[HMACSHA256, UserName], F] =
    TSecAuthService.withAuthorization(AdminRequired) {
      case _ @GET -> Root asAuthed _ =>
        Ok(assets.findAll)

      case req @ POST -> Root asAuthed _ =>
        req.request.decodeR[CreateAsset] { asset =>
          assets.create(asset.assetType.toDomain) *> Created()
        }

      case req @ PUT -> Root asAuthed _ =>
        req.request.decodeR[UpdateAsset] { asset =>
          assets.update(asset.toDomain) *> Ok()
        }

      case DELETE -> Root / LongVar(id) asAuthed _ =>
        assets.delete(AssetId(id)) *> NoContent()
    }

  def routes(
      secureRequestHandler: SecuredRequestHandler[F, UserName, User, AugmentedJWT[HMACSHA256, UserName]]
  ): HttpRoutes[F] =
    Router(
      prefixPath -> secureRequestHandler.liftService(httpRoutes)
    )
}
