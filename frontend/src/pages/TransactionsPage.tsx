import { getAsset } from '@/api/assets';
import { createTransaction, deleteTransaction, getTransactions } from '@/api/transactions';
import { Transaction, TransactionValue } from '@/api/types';
import { CURRENCIES } from '@/constants';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';

type BatchFields = { month: number; year: number };

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

const inputClass =
  'rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500';
const labelClass = 'text-sm font-medium text-slate-700';
const errorClass = 'text-xs text-rose-600';

export default function TransactionsPage() {
  const queryClient = useQueryClient();
  const outerForm = useForm<BatchFields>();
  const rowForm = useForm<TransactionValue>();

  const [stagedRows, setStagedRows] = useState<TransactionValue[]>([]);
  const [showForm, setShowForm] = useState(false);

  const rowFormId = useId();
  const batchFormId = useId();

  const createMutation = useMutation({
    mutationFn: createTransaction,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      toast.success('Transactions added');
      outerForm.reset();
      setStagedRows([]);
      setShowForm(false);
    },
    onError: (error) => {
      const message = error instanceof Error ? error.message : 'Failed to create transactions';
      toast.error(message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteTransaction,
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: ['transactions'] });
      const previous = queryClient.getQueryData<Transaction[]>(['transactions']);
      queryClient.setQueryData<Transaction[]>(['transactions'], (old) =>
        old?.filter((t) => t.transactionId !== id),
      );
      return { previous };
    },
    onError: (_err, _id, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['transactions'], context.previous);
      }
      toast.error('Delete failed — restored');
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
  });

  const queryTransactions = useQuery({
    queryKey: ['transactions'],
    queryFn: getTransactions,
  });

  const queryAssets = useQuery({
    queryKey: ['assets'],
    queryFn: getAsset,
  });

  const assetsById = new Map(queryAssets.data?.map((a) => [a.assetId, a]));

  const addRow = (row: TransactionValue) => {
    setStagedRows((prev) => [...prev, row]);
    rowForm.reset();
  };
  const removeRow = (i: number) =>
    setStagedRows((prev) => prev.filter((_, j) => j !== i));

  const submitBatch = (data: BatchFields) =>
    createMutation.mutate({ ...data, transactions: stagedRows });

  const rowErrors = rowForm.formState.errors;
  const batchErrors = outerForm.formState.errors;

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Transactions</h1>
          <p className="text-sm text-slate-500">A history of every movement in and out of your assets.</p>
        </div>
        <button
          type="button"
          onClick={() => setShowForm((s) => !s)}
          className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
        >
          {showForm ? 'Cancel' : '+ Add transactions'}
        </button>
      </header>

      {showForm && (
        <section
          aria-labelledby="add-tx-heading"
          className="space-y-6 rounded-lg border border-slate-200 bg-white p-6 shadow-sm"
        >
          <h2 id="add-tx-heading" className="text-lg font-medium">New batch</h2>

          {/* Row builder */}
          <form
            onSubmit={rowForm.handleSubmit(addRow)}
            noValidate
            className="grid grid-cols-1 gap-4 sm:grid-cols-4"
          >
            <div className="flex flex-col gap-1">
              <label htmlFor={`${rowFormId}-amount`} className={labelClass}>Amount</label>
              <input
                id={`${rowFormId}-amount`}
                type="number"
                step="0.01"
                inputMode="decimal"
                className={inputClass}
                aria-invalid={!!rowErrors.amount}
                {...rowForm.register('amount', { valueAsNumber: true, required: 'Required' })}
              />
              {rowErrors.amount && <span role="alert" className={errorClass}>{rowErrors.amount.message}</span>}
            </div>

            <div className="flex flex-col gap-1">
              <label htmlFor={`${rowFormId}-currency`} className={labelClass}>Currency</label>
              <select
                id={`${rowFormId}-currency`}
                defaultValue=""
                className={inputClass}
                aria-invalid={!!rowErrors.currency}
                {...rowForm.register('currency', { required: 'Required' })}
              >
                <option value="" disabled>--Select--</option>
                {CURRENCIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
              {rowErrors.currency && <span role="alert" className={errorClass}>{rowErrors.currency.message}</span>}
            </div>

            <div className="flex flex-col gap-1">
              <label htmlFor={`${rowFormId}-asset`} className={labelClass}>Asset</label>
              <select
                id={`${rowFormId}-asset`}
                defaultValue=""
                disabled={queryAssets.isLoading}
                className={inputClass}
                aria-invalid={!!rowErrors.assetId}
                {...rowForm.register('assetId', { valueAsNumber: true, required: 'Required' })}
              >
                <option value="" disabled>--Select--</option>
                {queryAssets.data?.map((a) => (
                  <option key={a.assetId} value={a.assetId}>
                    {a.assetName} ({a.assetType})
                  </option>
                ))}
              </select>
              {rowErrors.assetId && <span role="alert" className={errorClass}>{rowErrors.assetId.message}</span>}
            </div>

            <div className="flex items-end">
              <button
                type="submit"
                className="w-full rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Add row
              </button>
            </div>
          </form>

          {/* Staged rows */}
          {stagedRows.length > 0 && (
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <h3 className="mb-2 text-sm font-medium text-slate-700">Staged rows</h3>
              <ul className="divide-y divide-slate-200">
                {stagedRows.map((row, i) => {
                  const asset = assetsById.get(row.assetId);
                  return (
                    <li key={i} className="flex items-center justify-between py-2 text-sm">
                      <span>
                        {asset?.assetName} — {row.amount} {row.currency}
                      </span>
                      <button
                        type="button"
                        onClick={() => removeRow(i)}
                        className="text-xs text-rose-600 hover:text-rose-700"
                        aria-label={`Remove staged row ${i + 1}`}
                      >
                        Remove
                      </button>
                    </li>
                  );
                })}
              </ul>
            </div>
          )}

          {/* Batch period + submit */}
          <form
            onSubmit={outerForm.handleSubmit(submitBatch)}
            noValidate
            className="grid grid-cols-1 gap-4 border-t border-slate-200 pt-6 sm:grid-cols-3"
          >
            <div className="flex flex-col gap-1">
              <label htmlFor={`${batchFormId}-month`} className={labelClass}>Month</label>
              <select
                id={`${batchFormId}-month`}
                defaultValue=""
                className={inputClass}
                aria-invalid={!!batchErrors.month}
                {...outerForm.register('month', { valueAsNumber: true, required: 'Required' })}
              >
                <option value="" disabled>Select...</option>
                {MONTHS.map((m, i) => (
                  <option key={m} value={i + 1}>{m}</option>
                ))}
              </select>
              {batchErrors.month && <span role="alert" className={errorClass}>{batchErrors.month.message}</span>}
            </div>

            <div className="flex flex-col gap-1">
              <label htmlFor={`${batchFormId}-year`} className={labelClass}>Year</label>
              <input
                id={`${batchFormId}-year`}
                type="number"
                inputMode="numeric"
                className={inputClass}
                aria-invalid={!!batchErrors.year}
                {...outerForm.register('year', {
                  valueAsNumber: true,
                  required: 'Required',
                  min: { value: 1900, message: 'Year must be >= 1900' },
                  max: { value: 2100, message: 'Year must be <= 2100' },
                })}
              />
              {batchErrors.year && <span role="alert" className={errorClass}>{batchErrors.year.message}</span>}
            </div>

            <div className="flex items-end">
              <button
                type="submit"
                disabled={stagedRows.length === 0 || createMutation.isPending}
                className="w-full rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-indigo-700 disabled:opacity-50"
              >
                {createMutation.isPending ? 'Submitting…' : `Submit ${stagedRows.length} row${stagedRows.length === 1 ? '' : 's'}`}
              </button>
            </div>
          </form>
        </section>
      )}

      <section aria-labelledby="tx-list-heading">
        <h2 id="tx-list-heading" className="sr-only">All transactions</h2>

        {queryTransactions.isLoading && <p className="text-slate-500">Loading transactions…</p>}
        {queryTransactions.isError && <p role="alert" className="text-rose-600">Failed to load transactions.</p>}
        {queryTransactions.data && queryTransactions.data.length === 0 && (
          <div className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
            <p className="text-slate-500">No transactions yet.</p>
          </div>
        )}

        {queryTransactions.data && queryTransactions.data.length > 0 && (
          <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wide text-slate-500">Date</th>
                  <th className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wide text-slate-500">Asset</th>
                  <th className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wide text-slate-500">Type</th>
                  <th className="px-4 py-2 text-right text-xs font-medium uppercase tracking-wide text-slate-500">Amount</th>
                  <th className="px-4 py-2"><span className="sr-only">Actions</span></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {queryTransactions.data.map((t) => {
                  const asset = assetsById.get(t.assetId);
                  return (
                    <tr key={t.transactionId} className="hover:bg-slate-50">
                      <td className="whitespace-nowrap px-4 py-2 text-sm text-slate-700">
                        {String(t.month).padStart(2, '0')}/{t.year}
                      </td>
                      <td className="whitespace-nowrap px-4 py-2 text-sm text-slate-900">{asset?.assetName ?? '—'}</td>
                      <td className="whitespace-nowrap px-4 py-2 text-sm text-slate-500">{asset?.assetType ?? '—'}</td>
                      <td className="whitespace-nowrap px-4 py-2 text-right text-sm font-medium text-slate-900">{t.money}</td>
                      <td className="whitespace-nowrap px-4 py-2 text-right">
                        <button
                          type="button"
                          onClick={() => deleteMutation.mutate(t.transactionId)}
                          disabled={deleteMutation.isPending}
                          aria-label={`Delete transaction ${t.transactionId}`}
                          className="text-sm text-rose-600 hover:text-rose-700 disabled:opacity-50"
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
