import { createAsset, deleteAsset, getAsset } from '@/api/assets';
import { AssetType, CreateAssetRequest } from '@/api/types';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';

const ASSET_TYPES: AssetType[] = ['Loan', 'Cash', 'Investment', 'Property'];

export default function AssetsPage() {
  const queryClient = useQueryClient();

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
    },
    onError: (error) => {
      const message = error instanceof Error ? error.message : 'Failed to create asset';
      toast.error(message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteAsset,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      toast.success('Asset deleted');
    },
    onError: (error) => {
      const message = error instanceof Error ? error.message : 'Failed to delete asset';
      toast.error(message);
    },
  });

  const { data: assets, isLoading } = useQuery({
    queryKey: ['assets'],
    queryFn: getAsset,
  });

  return (
    <div>
      <h1>Assets</h1>
      {isLoading && <p>Loading...</p>}
      {assets && (
        <ul>
          {assets.map((a) => (
            <li key={a.assetId}>
              {a.assetName} ({a.assetType}){' '}
              <button onClick={() => deleteMutation.mutate(a.assetId)}>Delete</button>
            </li>
          ))}
        </ul>
      )}

      <h2>Create Asset</h2>
      <form onSubmit={handleSubmit((data) => createMutation.mutate(data))}>
        <label>asset type</label>
        <select
          {...register('assetType', {
            validate: (value) =>
              ASSET_TYPES.includes(value as AssetType) || 'Please select a valid status',
          })}
        >
          <option value="">Select...</option>
          {ASSET_TYPES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        {errors.assetType && <p>{errors.assetType.message}</p>}

        <label>asset name</label>
        <input
          {...register('assetName', {
            required: 'Asset name required',
          })}
        />
        {errors.assetName && <span>{errors.assetName.message}</span>}
        <button>{createMutation.isPending ? 'Creating...' : 'Create asset'}</button>
      </form>
    </div>
  );
}
