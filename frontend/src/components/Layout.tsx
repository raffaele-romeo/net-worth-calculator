import { useAuth } from "@/context/AuthContext";
  import { Link, Outlet } from "react-router";  

export default function Layout() {
    const { logout } = useAuth();

    return (
        <div>
            <nav style={{ padding: "1rem", borderBottom: "1px solid #ccc", display: "flex", gap: "1rem" }}>
            <Link to="/">Dashboard</Link>
            <Link to="/assets">Assets</Link>
            <Link to="/transactions">Transaction</Link>
            <button onClick={logout}>Logout</button>
            </nav>
            <main style={{ padding: "1rem" }}>
                <Outlet />
            </main>
        </div>
    )
}