import { createUser, loginUser, logoutUser } from "@/api/auth";
import { getToken } from "@/api/client";
import { createContext, ReactNode, useContext, useState } from "react";


type AuthContextValue = {
    isAuthenticated: boolean;
    login: (username: string, password: string) => Promise<void>;
    register: (username: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode}) {
    const [isAuthenticated, setIsAuthenticated] = useState(getToken() ? true : false);

    const login = async (username: string, password: string) => {
        await loginUser({username, password});
        setIsAuthenticated(true);
    }

    const register = async (username: string, password: string) => {
        await createUser({username, password});
        setIsAuthenticated(true);
    }

    const logout = async () => {
        await logoutUser();
        setIsAuthenticated(false);
    }

    const value: AuthContextValue = {isAuthenticated, login, register, logout};

    return <AuthContext value={value}>{children}</AuthContext>;
}

export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
}