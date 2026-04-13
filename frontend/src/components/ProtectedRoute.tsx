import { useAuth } from "@/context/AuthContext";
import { Navigate, Outlet } from "react-router";


export function ProtectedRoute() {
    const { isAuthenticated } = useAuth();
    if(!isAuthenticated) return <Navigate to="/login" replace />
    return <Outlet />;
}