package networthcalculator.http.routes.secured

import cats.effect.Concurrent
import cats.syntax.all.*
import cats.implicits.*
import networthcalculator.algebras.{AssetsService, ValidationService}
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
import cats.MonadThrow

final class AssetRoutes[F[_]: Concurrent: Logger](
    assets: AssetsService[F],
    validationService: ValidationService[F]
)(using ME: MonadThrow[F])
    extends Http4sDsl[F] {

  import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
  private[routes] val prefixPath = "/assets"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case _ @GET -> Root as user =>
      assets.findAll(user.userId).flatMap(assets => Ok(assets.asJson))

    case req @ POST -> Root as user =>
      req.req
        .decodeR[CreateAsset] { asset =>
          for {
            assetType <- validationService.validate(asset.assetType)
            result <- assets.create(
              assetType,
              asset.assetName,
              user.userId
            ) *> Created()
          } yield result
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
