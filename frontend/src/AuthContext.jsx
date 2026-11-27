import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import api, { setUnauthorizedHandler, tokenStore } from './api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(tokenStore.get())); // true while an existing token is being checked

  const clearSession = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(clearSession);
  }, [clearSession]);

  // Restore the session after a page reload by asking the server who the token belongs to.
  useEffect(() => {
    if (!tokenStore.get()) return;
    api.get('/api/auth/me')
      .then((res) => setUser(res.data))
      .catch(clearSession)
      .finally(() => setLoading(false));
  }, [clearSession]);

  const login = useCallback(async (email, password) => {
    const res = await api.post('/api/auth/login', { email, password });
    tokenStore.set(res.data.token);
    setUser(res.data.user);
    return res.data.user;
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/api/auth/logout'); // revokes the token on the server
    } catch {
      /* the local session is ended below either way */
    }
    clearSession();
  }, [clearSession]);

  const value = useMemo(() => ({ user, setUser, loading, login, logout }), [user, loading, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
