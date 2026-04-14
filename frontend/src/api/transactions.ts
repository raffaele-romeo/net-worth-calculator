import { get, post, remove } from './client';
import { AggregatedTransactions, CreateTransaction } from './types';

export function getTransactions(): Promise<AggregatedTransactions[]> {
  return get<AggregatedTransactions[]>('/transactions');
}

export function createTransaction(transaction: CreateTransaction): Promise<void> {
  return post<CreateTransaction, void>('/transactions', transaction);
}

export function deleteTransaction(id: number): Promise<void> {
  return remove(`/transactions/${id}`);
}

export function getNetWorth(year?: number, currency?: string): Promise<AggregatedTransactions[]> {
  const params = new URLSearchParams();
  if (year) params.set('year', year.toString());
  if (currency) params.set('currency', currency);
  const query = params.toString();
  return get<AggregatedTransactions[]>(`/transactions/net-worth${query ? `?${query}` : ''}`);
}

export function getTransactionsByAssetId(
  assetId: number,
  year?: number,
  currency?: string,
): Promise<AggregatedTransactions[]> {
  const params = new URLSearchParams();
  if (year) params.set('year', year.toString());
  if (currency) params.set('currency', currency);
  const query = params.toString();
  return get<AggregatedTransactions[]>(
    `/assets/${assetId}/transactions${query ? `?${query}` : ''}`,
  );
}

export function getTransactionsByAssetType(
  assetType: string,
  year?: number,
  currency?: string,
): Promise<AggregatedTransactions[]> {
  const params = new URLSearchParams();
  if (year) params.set('year', year.toString());
  if (currency) params.set('currency', currency);
  const query = params.toString();
  return get<AggregatedTransactions[]>(
    `/assets/${assetType}/transactions${query ? `?${query}` : ''}`,
  );
}
