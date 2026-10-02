import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { setUnauthorizedHandler, tokenStore } from '../api/client.js';
import { useToast } from './ToastContext.jsx';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(!!tokenStore.get());
  const navigate = useNavigate();
  const toast = useToast();

  const logout = useCallback((message) => {
    tokenStore.clear();
    setUser(null);
    if (message) toast.warning(message);
    navigate('/login', { replace: true });
  }, [navigate, toast]);

  // Expired / invalid token anywhere in the app -> back to login.
  useEffect(() => {
    setUnauthorizedHandler((msg) => {
      if (tokenStore.get()) logout(msg || 'Your session has expired. Please log in again.');
    });
  }, [logout]);

  // On page reload, restore the user from the saved token.
  useEffect(() => {
    if (!tokenStore.get()) return;
    api.get('/auth/me')
      .then((res) => setUser(res.data))
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false));
  }, []);

  const acceptAuth = (data) => {
    tokenStore.set(data.token);
    setUser(data.user);
    return data.user;
  };

  const login = async (username, password) => acceptAuth((await api.post('/auth/login', { username, password })).data);
  const register = async (form) => acceptAuth((await api.post('/auth/register', form)).data);

  const value = {
    user,
    loading,
    login,
    register,
    logout,
    isAdmin: user?.role === 'ADMIN',
    isLibrarian: user?.role === 'LIBRARIAN',
    isStaff: user?.role === 'ADMIN' || user?.role === 'LIBRARIAN',
    isMember: user?.role === 'MEMBER',
  };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
