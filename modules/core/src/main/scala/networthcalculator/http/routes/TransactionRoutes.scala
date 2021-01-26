package networthcalculator.http.routes

/*
import cats.{Defer, Monad}
import networthcalculator.algebras.Transactions
import networthcalculator.domain.auth.UserId
import org.http4s.HttpRoutes
import org.http4s.circe.{JsonDecoder, _}
import org.http4s.dsl.Http4sDsl
import networthcalculator.http.json._

final class TransactionRoutes[F[_]: Defer: JsonDecoder: Monad](
    transactions: Transactions[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/transactions"

  //TODO Transform into AuthedRoutes
  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    //The path parameter represents the user id for the time being
    case GET -> Root / LongVar(id) =>
      Ok(transactions.findAll(UserId(id)))

    case POST -> Root / LongVar(id) =>
  }

  def insert(userId: UserId, transaction: CreateTransaction): F[TransactionId]
  def bulkInsert(userId: UserId, transactions: List[CreateTransaction]): F[Unit]
  def update(userId: UserId, updateTransaction: UpdateTransaction): F[TransactionId]
  def delete(userId: UserId, transactionId: TransactionId): F[Unit]
  def findAll(userId: UserId): F[List[Transaction]] // When implementing, understand how to manage huge number. It can be a long list after a while
  def getTotalNetWorth(userId: UserId, totalNetWorth: FindTotalNetWorth): F[List[Statistics]]
  def getTrendNetWorth(userId: UserId, trendNetWorth: FindTrendNetWorth): F[List[Statistics]]
}
 */
