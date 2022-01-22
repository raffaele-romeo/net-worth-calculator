package networthcalculator.services

import cats.effect.{MonadCancelThrow, Resource}
import cats.implicits.*
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.postgres.*
import doobie.util.fragments.whereAndOpt
import networthcalculator.algebras.TransactionsService
import networthcalculator.domain.assets.*
import networthcalculator.domain.transactions.*
import networthcalculator.domain.users.*
import squants.market.{Money, MoneyContext}

import java.time.{Month, Year}

object TransactionServiceImpl {
  def make[F[_]: MonadCancelThrow](transactor: Resource[F, HikariTransactor[F]]) =
    new TransactionsService[F] {

      override def create(userId: UserId, transactions: List[ValidTransaction]): F[Unit] =
        // TODO Update to use Bulk Insert
        transactions
          .traverse_(transaction =>
            transactor
              .use(
                TransactionQueries
                  .insert(userId, transaction)
                  .exceptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
                    TransactionAlreadyCreated(
                      s"Transaction of ${transaction.month.getValue()}/${transaction.year.getValue()} already inserted in the system"
                    ).raiseError
                  }
                  .transact[F]
              )
          )

      override def delete(userId: UserId, transactionId: TransactionId): F[Unit] =
        transactor
          .use(
            TransactionQueries
              .delete(userId, transactionId)
              .transact[F]
          )
          .void

      override def findAll(userId: UserId): F[List[Transaction]] =
        transactor
          .use(
            TransactionQueries
              .select(userId)
              .transact[F]
          )

      override def totalNetWorthByCurrency(
          userId: UserId,
          maybeYear: Option[Year]
      )(using fxContext: MoneyContext): F[List[TotalNetWorthByCurrency]] = transactor.use(
        TransactionQueries
          .calculateTotalNetWorthByCurrency(userId, maybeYear)
          .transact[F]
      )

      override def netWorthByCurrencyAndAsset(userId: UserId, assetId: AssetId, year: Option[Year])(
          using fxContext: MoneyContext
      ): F[List[TotalNetWorthByCurrency]] = transactor.use(
        TransactionQueries
          .calculateNetWorthByCurrencyAndAsset(userId, assetId, year)
          .transact[F]
      )

      override def netWorthByCurrencyAndAssetType(
          userId: UserId,
          assetType: AssetType,
          year: Option[Year]
      )(using
          fxContext: MoneyContext
      ): F[List[TotalNetWorthByCurrency]] = transactor.use(
        TransactionQueries
          .calculateNetWorthByCurrencyAndAssetType(userId, assetType, year)
          .transact[F]
      )

    }
}

private object TransactionQueries {

  import Transaction.given

  def insert(userId: UserId, transaction: ValidTransaction): ConnectionIO[Int] = sql"""
       | INSERT INTO transactions (
       | amount,
       | currency,
       | month,
       | year,
       | asset_id,
       | user_id
       | )
       | VALUES (
       | ${transaction.money.amount},
       | ${transaction.money.currency.toString},
       | ${transaction.month.getValue()},
       | ${transaction.year.getValue()},
       | ${transaction.assetId.toLong},
       | ${userId.toLong}
       | )
         """.stripMargin.update.run

  def delete(userId: UserId, transactionId: TransactionId): ConnectionIO[Int] =
    sql"""
      | DELETE FROM transactions
      | WHERE id = ${transactionId.toLong} AND user_id = ${userId.toLong}
      """.stripMargin.update.run

  def select(userId: UserId): ConnectionIO[List[Transaction]] =
    sql"""
         | SELECT id, amount, currency, month, year, asset_id, user_id
         | FROM transactions
         | WHERE user_id = ${userId.toLong}
         | ORDER BY year DESC, month DESC;
      """.stripMargin.query[Transaction].to[List]

  def calculateTotalNetWorthByCurrency(
      userId: UserId,
      maybeYear: Option[Year]
  )(using fxContext: MoneyContext): ConnectionIO[List[TotalNetWorthByCurrency]] = {

    val f1Year = maybeYear.map(year => fr"year = ${year.getValue()}")
    val f2User = Some(userId).map(userId => fr"transactions.user_id = ${userId.toLong}")

    val queryFragment = fr"""
      |WITH relevant_transactions AS (
      |SELECT amount, currency, month, year, asset_name, asset_type 
      |FROM transactions 
      |INNER JOIN assets ON transactions.asset_id = assets.id   
    """.stripMargin ++ whereAndOpt(f2User, f1Year) ++
      fr""") 
      |SELECT SUM(CASE asset_type WHEN 'loan' THEN -amount ELSE amount END) as total, currency, month, year 
      |FROM relevant_transactions  
      |GROUP BY month, year, currency
      |ORDER BY year DESC, month DESC;
    """.stripMargin

    queryFragment.query[TotalNetWorthByCurrency].to[List]
  }

  def calculateNetWorthByCurrencyAndAsset(
      userId: UserId,
      assetId: AssetId,
      maybeYear: Option[Year]
  )(using fxContext: MoneyContext): ConnectionIO[List[TotalNetWorthByCurrency]] = {

    val f1Year  = maybeYear.map(year => fr"year = ${year.getValue()}")
    val f2User  = Some(userId).map(userId => fr"transactions.user_id = ${userId.toLong}")
    val f3Asset = Some(assetId).map(assetId => fr"transactions.asset_id = ${assetId.toLong}")

    val queryFragment = fr"""
      |WITH relevant_transactions AS (
      |SELECT amount, currency, month, year, asset_name, asset_type
      |FROM transactions 
      |INNER JOIN assets ON transactions.asset_id = assets.id   
    """.stripMargin ++ whereAndOpt(f2User, f3Asset, f1Year) ++
      fr""") 
      |SELECT SUM(amount), currency, month, year
      |FROM relevant_transactions  
      |GROUP BY month, year, currency
      |ORDER BY year DESC, month DESC;
    """.stripMargin

    queryFragment.query[TotalNetWorthByCurrency].to[List]
  }

  def calculateNetWorthByCurrencyAndAssetType(
      userId: UserId,
      assetType: AssetType,
      maybeYear: Option[Year]
  )(using fxContext: MoneyContext): ConnectionIO[List[TotalNetWorthByCurrency]] = {

    val f1Year = maybeYear.map(year => fr"year = ${year.getValue()}")
    val f2User = Some(userId).map(userId => fr"transactions.user_id = ${userId.toLong}")
    val f3Asset =
      Some(assetType).map(assetType => fr"asset_type = ${assetType.toString.toLowerCase}")

    val queryFragment = fr"""
      |WITH relevant_transactions AS (
      |SELECT amount, currency, month, year, asset_name, asset_type
      |FROM transactions 
      |INNER JOIN assets ON transactions.asset_id = assets.id   
    """.stripMargin ++ whereAndOpt(f2User, f3Asset, f1Year) ++
      fr""") 
      |SELECT SUM(amount), currency, month, year
      |FROM relevant_transactions  
      |GROUP BY month, year, currency
      |ORDER BY year DESC, month DESC;
    """.stripMargin

    queryFragment.query[TotalNetWorthByCurrency].to[List]
  }
}
