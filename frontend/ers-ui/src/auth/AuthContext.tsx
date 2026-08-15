import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { apiClient, tokenStorage, unwrap } from '../api/client';
import type { CurrentUser, TokenResponse } from '../api/types';

interface AuthContextValue {
  user: CurrentUser | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);

  const loadCurrentUser = useCallback(async () => {
    if (!tokenStorage.getAccessToken()) {
      setLoading(false);
      return;
    }
    try {
      const me = await unwrap(apiClient.get<{ success: boolean; data: CurrentUser; timestamp: string }>('/api/auth/me'));
      setUser(me);
    } catch {
      tokenStorage.clear();
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCurrentUser();
  }, [loadCurrentUser]);

  const login = useCallback(async (username: string, password: string) => {
    const tokens = await unwrap(
      apiClient.post<{ success: boolean; data: TokenResponse; timestamp: string }>('/api/auth/login', {
        username,
        password,
      })
    );
    tokenStorage.setTokens(tokens);
    await loadCurrentUser();
  }, [loadCurrentUser]);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  const hasRole = useCallback((role: string) => user?.roles.includes(role) ?? false, [user]);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}
