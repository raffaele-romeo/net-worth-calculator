import { createAsset, deleteAsset, getAsset } from '@/api/assets';
import { Asset, AssetType, CreateAssetRequest } from '@/api/types';
import { ASSET_TYPES } from '@/constants';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';

const TYPE_COLORS: Record<AssetType, string> = {
  Cash: 'bg-emerald-100 text-emerald-800',
  Investment: 'bg-indigo-100 text-indigo-800',
  Property: 'bg-amber-100 text-amber-800',
  Loan: 'bg-rose-100 text-rose-800',
};

export default function AssetsPage() {
  const queryClient = useQueryClient();
  const formId = useId();
  const [showForm, setShowForm] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateAssetRequest>();

  const createMutation = useMutation({
    mutationFn: createAsset,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      toast.success('Asset created');
      reset();
      setShowForm(false);
    },
    onError: (error) => {
      const message = error instanceof Error ? error.message : 'Failed to create asset';
      toast.error(message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteAsset,
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: ['assets'] });
      const previous = queryClient.getQueryData<Asset[]>(['assets']);
      queryClient.setQueryData<Asset[]>(['assets'], (old) =>
        old?.filter((a) => a.assetId !== id),
      );
      return { previous };
    },
    onError: (_err, _id, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['assets'], context.previous);
      }
      toast.error('Delete failed — restored');
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
    },
  });

  const { data: assets, isLoading, isError } = useQuery({
    queryKey: ['assets'],
    queryFn: getAsset,
  });

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Assets</h1>
          <p className="text-sm text-slate-500">Everything you own, organized by type.</p>
        </div>
        <button
          type="button"
          onClick={() => setShowForm((s) => !s)}
          className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
        >
          {showForm ? 'Cancel' : '+ Add asset'}
        </button>
      </header>

      {showForm && (
        <section
          aria-labelledby="create-asset-heading"
          className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm"
        >
          <h2 id="create-asset-heading" className="mb-4 text-lg font-medium">
            Create asset
          </h2>
          <form
            onSubmit={handleSubmit((data) => createMutation.mutate(data))}
            noValidate
            className="grid grid-cols-1 gap-4 sm:grid-cols-2"
          >
            <div className="flex flex-col gap-1">
              <label htmlFor={`${formId}-type`} className="text-sm font-medium text-slate-700">
                Asset type
              </label>
              <select
                id={`${formId}-type`}
                defaultValue=""
                aria-invalid={!!errors.assetType}
                aria-describedby={errors.assetType ? `${formId}-type-error` : undefined}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                {...register('assetType', {
                  validate: (v) =>
                    ASSET_TYPES.includes(v as AssetType) || 'Please select a valid type',
                })}
              >
                <option value="" disabled>Select...</option>
                {ASSET_TYPES.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
              {errors.assetType && (
                <span id={`${formId}-type-error`} role="alert" className="text-xs text-rose-600">
                  {errors.assetType.message}
                </span>
              )}
            </div>

            <div className="flex flex-col gap-1">
              <label htmlFor={`${formId}-name`} className="text-sm font-medium text-slate-700">
                Asset name
              </label>
              <input
                id={`${formId}-name`}
                autoComplete="off"
                aria-invalid={!!errors.assetName}
                aria-describedby={errors.assetName ? `${formId}-name-error` : undefined}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                {...register('assetName', { required: 'Asset name required' })}
              />
              {errors.assetName && (
                <span id={`${formId}-name-error`} role="alert" className="text-xs text-rose-600">
                  {errors.assetName.message}
                </span>
              )}
            </div>

            <div className="sm:col-span-2">
              <button
                type="submit"
                disabled={createMutation.isPending}
                className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
              >
                {createMutation.isPending ? 'Creating…' : 'Create asset'}
              </button>
            </div>
          </form>
        </section>
      )}

      <section aria-labelledby="assets-list-heading">
        <h2 id="assets-list-heading" className="sr-only">All assets</h2>

        {isLoading && <p className="text-slate-500">Loading assets…</p>}
        {isError && <p role="alert" className="text-rose-600">Failed to load assets.</p>}
        {assets && assets.length === 0 && (
          <div className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
            <p className="text-slate-500">No assets yet. Click “Add asset” to create your first one.</p>
          </div>
        )}

        {assets && assets.length > 0 && (
          <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {assets.map((a) => (
              <li
                key={a.assetId}
                className="flex flex-col rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition hover:shadow-md"
              >
                <div className="flex items-start justify-between gap-2">
                  <h3 className="text-base font-medium text-slate-900">{a.assetName}</h3>
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${TYPE_COLORS[a.assetType]}`}
                  >
                    {a.assetType}
                  </span>
                </div>
                <div className="mt-4 flex justify-end">
                  <button
                    type="button"
                    onClick={() => deleteMutation.mutate(a.assetId)}
                    disabled={deleteMutation.isPending}
                    aria-label={`Delete asset ${a.assetName}`}
                    className="text-sm text-rose-600 hover:text-rose-700 disabled:opacity-50"
                  >
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
