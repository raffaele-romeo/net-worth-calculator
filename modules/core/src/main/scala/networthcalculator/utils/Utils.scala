package networthcalculator.utils

import networthcalculator.domain.transactions.AggregatedTransactions

object Utils:
  def sort(
    aggregatedTransactions: List[AggregatedTransactions]
  ): List[AggregatedTransactions] =
    aggregatedTransactions
      .sorted(
        Ordering
          .by(
            (
              (totalNetWorth: AggregatedTransactions) =>
                (totalNetWorth.year, totalNetWorth.month)
            )
          )
          .reverse
      )
