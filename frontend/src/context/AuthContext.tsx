import { createUser, loginUser, logoutUser } from '@/api/auth';
import { getToken, getTokenExp, setToken } from '@/api/client';
import { createContext, ReactNode, useCallback, useContext, useEffect, useRef, useState } from 'react';
import toast from 'react-hot-toast';

const WARN_BEFORE_MS = 2 * 60 * 1000; // warn 2 minutes before expiry

type AuthContextValue = {
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(() => {
    const tok = getToken();
    if (!tok) return false;
    const exp = getTokenExp(tok);
    if (exp && exp * 1000 <= Date.now()) {
      setToken(null);
      return false;
    }
    return true;
  });

  const logoutTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const warnTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearTimers = useCallback(() => {
    if (logoutTimerRef.current) {
      clearTimeout(logoutTimerRef.current);
      logoutTimerRef.current = null;
    }
    if (warnTimerRef.current) {
      clearTimeout(warnTimerRef.current);
      warnTimerRef.current = null;
    }
  }, []);

  const scheduleAutoLogout = useCallback(
    (token: string) => {
      clearTimers();
      const exp = getTokenExp(token);
      if (!exp) return;

      const msUntilExpiry = exp * 1000 - Date.now();
      if (msUntilExpiry <= 0) {
        setToken(null);
        setIsAuthenticated(false);
        return;
      }

      logoutTimerRef.current = setTimeout(() => {
        setToken(null);
        setIsAuthenticated(false);
        toast.error('Session expired — please log in again');
      }, msUntilExpiry);

      const msUntilWarn = msUntilExpiry - WARN_BEFORE_MS;
      if (msUntilWarn > 0) {
        warnTimerRef.current = setTimeout(() => {
          toast('Your session expires in 2 minutes', { icon: '\u23f3' });
        }, msUntilWarn);
      }
    },
    [clearTimers],
  );

  // Schedule timers on mount if a valid token exists
  useEffect(() => {
    const tok = getToken();
    if (tok && isAuthenticated) {
      scheduleAutoLogout(tok);
    }
    return clearTimers;
  }, [isAuthenticated, scheduleAutoLogout, clearTimers]);

  const login = async (username: string, password: string) => {
    const token = await loginUser({ username, password });
    setIsAuthenticated(true);
    scheduleAutoLogout(token);
  };

  const register = async (username: string, password: string) => {
    const token = await createUser({ username, password });
    setIsAuthenticated(true);
    scheduleAutoLogout(token);
  };

  const logout = async () => {
    clearTimers();
    try {
      await logoutUser();
    } finally {
      setIsAuthenticated(false);
    }
  };

  const value: AuthContextValue = { isAuthenticated, login, register, logout };

  return <AuthContext value={value}>{children}</AuthContext>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
