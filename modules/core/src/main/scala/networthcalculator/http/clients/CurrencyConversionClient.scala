package networthcalculator.http.clients

import networthcalculator.domain.currencyconversion.*

import java.util.UUID
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.client.dsl.Http4sClientDsl
import networthcalculator.config.data.CurrencyConversionConfig
import org.typelevel.log4cats.Logger
import cats.effect.*
import cats.implicits.*
import io.circe.generic.auto.*
import org.http4s.Status.NotFound
import org.http4s.Status.Successful
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.syntax.all.*
import cats.*
import io.circe.Json
import io.circe.parser.parse
import org.http4s.Uri
import squants.market.{Currency, CurrencyExchangeRate, MoneyContext, defaultMoneyContext}

trait CurrencyConversionClient[F[_]] {
  def latestRates(
      baseCurrency: String,
      dateFrom: Option[String]
  ): F[String]
}

object CurrencyConversionClient {
  def make[F[_]: JsonDecoder: Sync: Logger](
      currencyConversionConfig: CurrencyConversionConfig,
      client: Client[F]
  )(using ME: MonadThrow[F]) = new CurrencyConversionClient[F] with Http4sClientDsl[F] {
    override def latestRates(
        baseCurrency: String,
        dateFrom: Option[String]
    ): F[String] = {

      val baseUri = uri"https://freecurrencyapi.net/api/v2/latest"
      val withQuery = baseUri
        .withQueryParam("apikey", currencyConversionConfig.apiKey.toString)
        .withQueryParam("base_currency", baseCurrency)
        .withQueryParam("date_from", dateFrom.fold(java.time.LocalDate.now.toString)(identity))

      for {
        currencyConversion <- client.get(withQuery) {
          case Successful(resp) =>
            Sync[F].pure(resp.body.toString)
          case resp =>
            CurrencyConversionError(
              resp.status.toString
            ).raiseError[F, String]
        }
      } yield currencyConversion
    }

//    private def createExchangeRates(
//        baseCurrency: String,
//        currencyConversion: String
//    ): List[CurrencyExchangeRate] = {
//      given MoneyContext = defaultMoneyContext
//
//      val doc = parse(currencyConversion).getOrElse(Json.Null)
//
//      val currencyConversionToMap =
//        doc.\\("data").head.asObject.get.toMap.map { (name, value) => (name, value.asNumber.get) }
//
//      val availableCurrency = currencyConversionToMap.filter { (name, value) =>
//        Currency(name.toString).isSuccess
//      }
//
//      availableCurrency.map { (name, value) =>
//        Currency(baseCurrency).get / (Currency(name.toString).get)(value.toBigDecimal)
//      }
//    }
  }
}

object ClientExample extends IOApp {
  import org.http4s.blaze.client.BlazeClientBuilder
  import org.typelevel.log4cats
  import org.typelevel.log4cats.Logger
  import org.typelevel.log4cats.slf4j.Slf4jLogger

//   def createExchangeRates(currencyConversion: CurrencyConversion): List[CurrencyExchangeRate] = {
//     given MoneyContext = defaultMoneyContext
//     val baseCurrency   = Currency(currencyConversion.query.base_currency)

//     val availableCurrency = currencyConversion.data.filter { (name, value) =>
//       Currency(name.toString).isSuccess
//     }

//     availableCurrency.map { (name, value) =>
//       baseCurrency.get / (Currency(name.toString).get)(value.toBigDecimal)
//     }
//   }

  given unsafeLogger: log4cats.SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  import cats.syntax.either._
  import io.circe._, io.circe.parser._

  def run(args: List[String]): IO[ExitCode] = IO {
    val json =
      """
        |{
        |  "query" : {
        |    "apikey" : "ce51a410-7bbd-11ec-a2fe-b7a0c5b16d51",
        |    "base_currency" : "GBP",
        |    "date_from" : "2022-01-22",
        |    "timestamp" : 1642892188
        |  },
        |  "data" : {
        |    "USD" : 1.355528,
        |    "JPY" : 154.079,
        |    "NZD" : 2.019032
        |  }
        |}
        |""".stripMargin

    import io.circe.Decoder

    case class Cars(cars: List[Car])

    object Cars {
      implicit val decodeCars: Decoder[Cars] =
        Decoder[Map[String, CarDetails]]
          .prepare(_.downField("cars"))
          .map(kvs =>
            Cars(
              kvs.map { case (k, v) =>
                Car(k, v)
              }.toList
            )
          )
    }

    // I've added an `id` member here as a way to hold on to the JSON key.
    case class Car(id: String, whatShouldThisBe: CarDetails)

    case class CarDetails(name: String)

    import io.circe._, io.circe.generic.semiauto._

    object CarDetails {
      implicit val fooDecoder: Decoder[CarDetails] = deriveDecoder
      implicit val fooEncoder: Encoder[CarDetails] = deriveEncoder
    }

    val cars = """
        |     {
        |       "cars": {
        |          "THIS IS A DYNAMIC KEY 1": {
        |            "name": "bla 1"
        |          },
        |          "THIS IS A DYNAMIC KEY 2": {
        |            "name": "bla 2"
        |          }
      |          }
        |      }
        |""".stripMargin

    val doc = parse(cars).getOrElse(Json.Null).as[Cars]

    println(parse(cars).getOrElse(Json.Null))
    println(doc)

  }.as(ExitCode.Success)
//    BlazeClientBuilder[IO].resource
//      .use { client =>
//        CurrencyConversionClient
//          .make[IO](
//            CurrencyConversionConfig(apiKey =
//              UUID.fromString("ce51a410-7bbd-11ec-a2fe-b7a0c5b16d51")
//            ),
//            client
//          )
//          .latestRates("GBP", None)
//          .map(println)
//      }
//      .as(ExitCode.Success)
}
