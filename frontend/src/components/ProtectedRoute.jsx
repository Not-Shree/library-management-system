import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { Loading } from './Common.jsx';

/**
 * Blocks pages for visitors who are not logged in, or whose role is not in `roles`.
 * The backend checks roles again on every API call; this only keeps the UI tidy.
 */
export default function ProtectedRoute({ roles }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <Loading text="Checking your session…" />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (roles && !roles.includes(user.role)) {
    return (
      <div className="container py-5 text-center">
        <i className="bi bi-shield-lock display-5 text-body-tertiary" aria-hidden="true" />
        <h1 className="h4 mt-3">This page is not available for your account</h1>
        <p className="text-body-secondary">Ask an administrator if you need access.</p>
      </div>
    );
  }
  return <Outlet />;
}
