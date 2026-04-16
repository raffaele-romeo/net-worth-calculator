import { useAuth } from '@/context/AuthContext';
import { Link, Outlet } from 'react-router';

export default function Layout() {
  const { logout } = useAuth();

  const linkClass =
    'rounded-md px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900';

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <nav className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center gap-2 px-4 py-3">
          <span className="mr-4 text-lg font-semibold">Net Worth</span>
          <Link to="/" className={linkClass}>Dashboard</Link>
          <Link to="/assets" className={linkClass}>Assets</Link>
          <Link to="/transactions" className={linkClass}>Transactions</Link>
          <button
            type="button"
            onClick={logout}
            className="ml-auto rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Logout
          </button>
        </div>
      </nav>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  );
}
