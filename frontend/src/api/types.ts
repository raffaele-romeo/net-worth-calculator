export type AssetType = 'Loan' | 'Cash' | 'Investment' | 'Property';

export type Asset = {
  assetId: number;
  assetType: AssetType;
  assetName: string;
  userId: number;
};

export type CreateAssetRequest = {
  assetType: string;
  assetName: string;
};

export type AppStatus = {
  redis: boolean;
  postgres: boolean;
};

export type Transaction = {
  transactionId: number;
  money: string;
  month: number;
  year: number;
  assetId: number;
  userId: number;
};

export type User = {
  username: string;
  password: string;
};

export type TransactionValue = {
  amount: number;
  currency: string;
  assetId: number;
};

export type CreateTransaction = {
  month: number;
  year: number;
  transactions: TransactionValue[];
};

export type AggregatedTransactions = {
  totals: string[];
  month: number;
  year: number;
};

export type NetWorthFilters = {
  year?: number;
  currency?: string;
};

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}
