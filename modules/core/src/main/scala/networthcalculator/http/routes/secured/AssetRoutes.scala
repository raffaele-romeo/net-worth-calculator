package networthcalculator.http.routes.secured

import cats.effect.Concurrent
import cats.implicits.*
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import networthcalculator.algebras.{AssetsService, ValidationService}
import networthcalculator.domain.assets.*
import networthcalculator.domain.users.CommonUser
import networthcalculator.http.decoder.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger

final class AssetRoutes[F[_]: Concurrent: Logger](
    assets: AssetsService[F],
    validationService: ValidationService[F]
)
    extends Http4sDsl[F] {

  import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
  private[routes] val prefixPath = "/assets"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case _ @GET -> Root as user =>
      for {
        assets   <- assets.findAll(user.userId)
        response <- Ok(assets.asJson)
      } yield response

    case req @ POST -> Root as user =>
      req.req
        .decodeR[CreateAsset] { asset =>
          for {
            assetType <- validationService.validate(asset.assetType)
            _ <- assets.create(
              assetType,
              asset.assetName,
              user.userId
            )
            response <- Created()
          } yield response
        }
        .recoverWith {
          case AssetTypeNotAllowed(error) => BadRequest(error.asJson)
          case AssetAlreadyInUse(error)   => BadRequest(error.asJson)
        }

    case DELETE -> Root / LongVar(id) as user =>
      assets.delete(AssetId(id), user.userId) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
