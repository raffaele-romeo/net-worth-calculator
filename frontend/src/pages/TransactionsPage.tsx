import { getAsset } from "@/api/assets";
import { createTransaction, deleteTransaction, getTransactions } from "@/api/transactions";
import { TransactionValue } from "@/api/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import toast from "react-hot-toast";

type BatchFields = { month: number; year: number };

export default function TransactionsPage() {
  const queryClient = useQueryClient();

  const outerForm = useForm<BatchFields>();
  const rowForm = useForm<TransactionValue>();

  const [stagedRows, setStagedRows] = useState<TransactionValue[]>([]);

  const createMutation = useMutation({
    mutationFn: createTransaction,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      toast.success("Transactions added");
      outerForm.reset();
      setStagedRows([]);
    },
    onError: (error) => {
      const message = error instanceof Error ? error.message : 'Failed to create transactions';
      toast.error(message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteTransaction,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      toast.success('Transaction deleted');
    },
    onError: (error) => {
      const message = error instanceof Error ? error.message : 'Failed to delete transaction';
      toast.error(message);
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

  const months = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  ];

  const addRow = (row: TransactionValue) => {
    setStagedRows((prev) => [...prev, row]);
    rowForm.reset();
  };

  const removeRow = (index: number) => {
    setStagedRows((prev) => prev.filter((_, i) => i !== index));
  };

  const submitBatch = (data: BatchFields) => {
    createMutation.mutate({ ...data, transactions: stagedRows });
  };

  return (
    <div>
      <h1>Transactions</h1>
      {queryTransactions.isLoading && <p>Transactions loading...</p>}
      {queryAssets.isLoading && <p>Assets loading...</p>}

      {queryTransactions.data && (
        <ul>
          {queryTransactions.data.map((t) => {
            const asset = assetsById.get(t.assetId);
            return (
              <li key={t.transactionId}>
                {t.month}/{t.year} — {asset?.assetType}: {asset?.assetName} — {t.money}{' '}
                <button onClick={() => deleteMutation.mutate(t.transactionId)}>Delete</button>
              </li>
            );
          })}
        </ul>
      )}

      <h2>Add a row</h2>
      <form onSubmit={rowForm.handleSubmit(addRow)}>
        <label>amount</label>
        <input
          type="number"
          step="0.01"
          {...rowForm.register('amount', { valueAsNumber: true, required: 'Amount required' })}
        />
        {rowForm.formState.errors.amount && <span>{rowForm.formState.errors.amount.message}</span>}

        <label>currency</label>
        <input
          {...rowForm.register('currency', { 
            required: 'Currency required',
            setValueAs: (v: string) => v?.trim().toUpperCase(),
          })}
        />
        {rowForm.formState.errors.currency && <span>{rowForm.formState.errors.currency.message}</span>}

        <label>asset</label>
        <select
          defaultValue=""
          {...rowForm.register('assetId', { valueAsNumber: true, required: 'Asset required' })}
        >
          <option value="" disabled>--Select--</option>
          {queryAssets.data?.map((a) => (
            <option key={a.assetId} value={a.assetId}>
              {a.assetName} ({a.assetType})
            </option>
          ))}
        </select>
        {rowForm.formState.errors.assetId && <span>{rowForm.formState.errors.assetId.message}</span>}{' '}

        <button type="submit">Add</button>
      </form>

      {stagedRows.length > 0 && (
        <>
          <h3>Staged rows</h3>
          <ul>
            {stagedRows.map((row, i) => {
              const asset = assetsById.get(row.assetId);
              return (
                <li key={i}>
                  {asset?.assetName} — {row.amount} {row.currency}{' '}
                  <button type="button" onClick={() => removeRow(i)}>Remove</button>
                </li>
              );
            })}
          </ul>
        </>
      )}

      <h2>Submit batch</h2>
      <form onSubmit={outerForm.handleSubmit(submitBatch)}>
        <label>month</label>
        <select
          defaultValue=""
          {...outerForm.register('month', { valueAsNumber: true, required: 'Month required' })}
        >
          <option value="" disabled>Select...</option>
          {months.map((month, index) => (
            <option key={month} value={index + 1}>
              {month}
            </option>
          ))}
        </select>
        {outerForm.formState.errors.month && <p>{outerForm.formState.errors.month.message}</p>}

        <label>year</label>
        <input
          type="number"
          {...outerForm.register('year', {
            valueAsNumber: true,
            required: 'Year required',
            min: { value: 1900, message: 'Year must be >= 1900' },
            max: { value: 2100, message: 'Year must be <= 2100' },
          })}
        />
        {outerForm.formState.errors.year && <span>{outerForm.formState.errors.year.message}</span>}

        <button type="submit" disabled={stagedRows.length === 0}>Submit</button>
      </form>
    </div>
  );
}
