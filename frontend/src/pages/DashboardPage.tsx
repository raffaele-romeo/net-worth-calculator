
import { getNetWorth, getTransactionsByAssetId, getTransactionsByAssetType } from '@/api/transactions';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';

export default function DashboardPage() {
  const [yearFilter, setYearFilter] = useState<number | undefined>();
  const [currencyFilter, setCurrencyFilter] = useState<string | undefined>();
  const [assetId, setAssetId] = useState<number | undefined>();
  const [assetType, setAssetType] = useState<string | undefined>();

  const netWorth = useQuery({
    queryKey: ['netWorth', yearFilter, currencyFilter],
    queryFn: () => getNetWorth(yearFilter, currencyFilter),
  });


  const trasactionsByAssetId = useQuery({
    queryKey: ['netWorth', yearFilter, currencyFilter],
    queryFn: () => getTransactionsByAssetId(yearFilter, currencyFilter),
  });


  const transactionsByAssetType = useQuery({
    queryKey: ['netWorth', yearFilter, currencyFilter],
    queryFn: () => getTransactionsByAssetType(yearFilter, currencyFilter),
  });



  return (
    <div>
      <h1>Dashboard</h1>
      <div>
        <h2>Net Worth</h2>
        {netWorth.isLoading && <p>Loading...</p>}
        {netWorth.isError && <p>Error loading assets</p>}
        {netWorth.data && (
          <ul>
            {netWorth.data.map((n) => (
              <li key={`${n.month}-${n.year}`}>
                 {n.year}-{String(n.month).padStart(2, '0')}: {n.totals.join(" ")}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
