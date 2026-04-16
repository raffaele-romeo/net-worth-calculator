import { getAsset } from '@/api/assets';
import {
  getNetWorth,
  getTransactionsByAssetId,
  getTransactionsByAssetType,
} from '@/api/transactions';
import { AggregatedTransactions, AssetType } from '@/api/types';
import { ASSET_TYPES, CURRENCIES } from '@/constants';
import { useQuery } from '@tanstack/react-query';
import { useId, useState } from 'react';

const inputClass =
  'rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500';
const labelClass = 'text-sm font-medium text-slate-700';

function ResultList({ data }: { data: AggregatedTransactions[] | undefined }) {
  if (!data) return null;
  if (data.length === 0) return <p className="text-sm text-slate-500">No results.</p>;
  return (
    <ul className="divide-y divide-slate-200 rounded-md border border-slate-200 bg-white">
      {data.map((n) => (
        <li key={`${n.month}-${n.year}`} className="flex justify-between px-4 py-2 text-sm">
          <span className="text-slate-500">
            {n.year}-{String(n.month).padStart(2, '0')}
          </span>
          <span className="font-medium text-slate-900">{n.totals.join(' ')}</span>
        </li>
      ))}
    </ul>
  );
}

function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-medium text-slate-900">{title}</h2>
      {children}
    </section>
  );
}

export default function DashboardPage() {
  const filtersId = useId();
  const byAssetId = useId();
  const byTypeId = useId();

  const [yearFilter, setYearFilter] = useState<number | undefined>();
  const [currencyFilter, setCurrencyFilter] = useState<string | undefined>();
  const [assetId, setAssetId] = useState<number | undefined>();
  const [assetType, setAssetType] = useState<AssetType | undefined>();

  const queryAssets = useQuery({ queryKey: ['assets'], queryFn: getAsset });

  const netWorth = useQuery({
    queryKey: ['netWorth', yearFilter, currencyFilter],
    queryFn: () => getNetWorth(yearFilter, currencyFilter),
  });

  const transactionsByAssetId = useQuery({
    queryKey: ['transactionsByAssetId', assetId, yearFilter, currencyFilter],
    queryFn: () => getTransactionsByAssetId(assetId!, yearFilter, currencyFilter),
    enabled: assetId !== undefined,
  });

  const transactionsByAssetType = useQuery({
    queryKey: ['transactionsByAssetType', assetType, yearFilter, currencyFilter],
    queryFn: () => getTransactionsByAssetType(assetType!, yearFilter, currencyFilter),
    enabled: assetType !== undefined,
  });

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold">Dashboard</h1>
        <p className="text-sm text-slate-500">Your net worth, broken down.</p>
      </header>

      <Panel title="Filters">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1">
            <label htmlFor={`${filtersId}-year`} className={labelClass}>Year</label>
            <input
              id={`${filtersId}-year`}
              type="number"
              inputMode="numeric"
              min={1900}
              max={2100}
              className={inputClass}
              value={yearFilter ?? ''}
              onChange={(e) =>
                setYearFilter(e.target.value ? Number(e.target.value) : undefined)
              }
              placeholder="all years"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor={`${filtersId}-currency`} className={labelClass}>Currency</label>
            <select
              id={`${filtersId}-currency`}
              className={inputClass}
              value={currencyFilter ?? ''}
              onChange={(e) => setCurrencyFilter(e.target.value || undefined)}
            >
              <option value="">all currencies</option>
              {CURRENCIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
        </div>
      </Panel>

      <Panel title="Net worth">
        {netWorth.isLoading && <p className="text-sm text-slate-500">Loading…</p>}
        {netWorth.isError && <p role="alert" className="text-sm text-rose-600">Failed to load net worth.</p>}
        <ResultList data={netWorth.data} />
      </Panel>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Panel title="By asset">
          <div className="mb-4 flex flex-col gap-1">
            <label htmlFor={`${byAssetId}-asset`} className={labelClass}>Asset</label>
            <select
              id={`${byAssetId}-asset`}
              className={inputClass}
              disabled={queryAssets.isLoading}
              value={assetId ?? ''}
              onChange={(e) =>
                setAssetId(e.target.value ? Number(e.target.value) : undefined)
              }
            >
              <option value="">--Select asset--</option>
              {queryAssets.data?.map((a) => (
                <option key={a.assetId} value={a.assetId}>
                  {a.assetName} ({a.assetType})
                </option>
              ))}
            </select>
          </div>
          {transactionsByAssetId.isLoading && <p className="text-sm text-slate-500">Loading…</p>}
          {transactionsByAssetId.isError && (
            <p role="alert" className="text-sm text-rose-600">Failed to load transactions.</p>
          )}
          <ResultList data={transactionsByAssetId.data} />
        </Panel>

        <Panel title="By asset type">
          <div className="mb-4 flex flex-col gap-1">
            <label htmlFor={`${byTypeId}-type`} className={labelClass}>Type</label>
            <select
              id={`${byTypeId}-type`}
              className={inputClass}
              value={assetType ?? ''}
              onChange={(e) =>
                setAssetType((e.target.value || undefined) as AssetType | undefined)
              }
            >
              <option value="">--Select type--</option>
              {ASSET_TYPES.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>
          {transactionsByAssetType.isLoading && <p className="text-sm text-slate-500">Loading…</p>}
          {transactionsByAssetType.isError && (
            <p role="alert" className="text-sm text-rose-600">Failed to load transactions.</p>
          )}
          <ResultList data={transactionsByAssetType.data} />
        </Panel>
      </div>
    </div>
  );
}
