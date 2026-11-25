import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import * as authApi from '../api/auth.js';
import { onUnauthorized } from '../api/http.js';

/** @typedef {import('../api/types.js').CurrentUser} CurrentUser */
/** @typedef {import('../api/types.js').UserRole} UserRole */

const AuthContext = createContext(null);

/**
 * Holds the signed-in user. The session itself is an HttpOnly cookie; here we only keep
 * who the user is and their role, for navigation (the backend enforces permissions).
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(/** @type {CurrentUser | null} */ (null));
  const [status, setStatus] = useState(/** @type {'loading' | 'ready'} */ ('loading'));

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const current = await authApi.fetchCurrentUser();
        if (active) setUser(current);
      } catch {
        if (active) setUser(null);
      } finally {
        if (active) setStatus('ready');
      }
    })();
    const unsubscribe = onUnauthorized(() => setUser(null));
    return () => {
      active = false;
      unsubscribe();
    };
  }, []);

  const login = useCallback(async (username, password) => {
    const current = await authApi.login(username, password);
    setUser(current);
    return current;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
    }
  }, []);

  const value = useMemo(() => ({ user, status, login, logout }), [user, status, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * @returns {{ user: CurrentUser | null, status: 'loading' | 'ready',
 *   login: (username: string, password: string) => Promise<CurrentUser>,
 *   logout: () => Promise<void> }}
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}

/**
 * @param {CurrentUser | null} user
 * @param {UserRole[] | undefined} roles
 * @returns {boolean} Whether the user may see something restricted to `roles`.
 */
export function hasRole(user, roles) {
  if (!roles) return Boolean(user);
  return Boolean(user && roles.includes(user.role));
}
