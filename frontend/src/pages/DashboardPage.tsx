import { fetchHealth } from '@/api/health';
import { getNetWorth } from '@/api/transactions';
import { useQuery } from '@tanstack/react-query';

export default function DashboardPage() {
  const health = useQuery({
    queryKey: ['health'],
    queryFn: fetchHealth,
  });

  const netWorth = useQuery({
    queryKey: ['netWorth'],
    queryFn: () => getNetWorth(),
  });

  return (
    <div>
      <h1>Dashboard</h1>
      <div>
        <h2>System Health</h2>
        {health.isLoading && <p>Loading...</p>}
        {health.isError && <p>Error loading health</p>}
        {health.data && (
          <>
            <p>
              Postgres: <strong>{health.data?.postgres ? 'up' : 'down'}</strong>
            </p>
            <p>
              Redis: <strong>{health.data?.redis ? 'up' : 'down'}</strong>
            </p>
          </>
        )}
      </div>

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
