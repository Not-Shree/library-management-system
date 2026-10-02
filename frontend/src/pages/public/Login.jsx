import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { errorMessage } from '../../api/client.js';
import AuthShell from './AuthShell.jsx';

// Demo accounts from database/seed.sql — for local testing only.
const DEMO = [
  { role: 'Admin', username: 'admin', password: 'Admin@123' },
  { role: 'Librarian', username: 'librarian', password: 'Librarian@123' },
  { role: 'Student', username: 'student', password: 'Student@123' },
];

export default function Login() {
  const { login, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ username: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/dashboard" replace />;

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(form.username.trim(), form.password);
      navigate(location.state?.from || '/dashboard', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthShell title="Log in" subtitle="Use your library username or email.">
      {error && <div className="alert alert-danger py-2" role="alert">{error}</div>}
      <form onSubmit={submit} noValidate>
        <div className="mb-3">
          <label className="form-label" htmlFor="username">Username or email</label>
          <input id="username" className="form-control form-control-lg" autoComplete="username" required
            value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
        </div>
        <div className="mb-2">
          <label className="form-label" htmlFor="password">Password</label>
          <div className="input-group">
            <input id="password" type={showPassword ? 'text' : 'password'} className="form-control form-control-lg"
              autoComplete="current-password" required value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })} />
            <button type="button" className="btn btn-outline-secondary" onClick={() => setShowPassword(!showPassword)}
              aria-label={showPassword ? 'Hide password' : 'Show password'}>
              <i className={`bi bi-eye${showPassword ? '-slash' : ''}`} />
            </button>
          </div>
        </div>
        <div className="text-end mb-4"><Link to="/forgot-password" className="small">Forgot password?</Link></div>
        <button className="btn btn-primary btn-lg w-100" disabled={busy || !form.username || !form.password}>
          {busy && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}Log in
        </button>
      </form>
      <p className="mt-4 mb-0 text-center">New student or staff member? <Link to="/register">Create a library account</Link></p>

      <div className="demo-box mt-4">
        <div className="small fw-semibold mb-2">Demo accounts <span className="text-body-secondary fw-normal">(local testing only — change these in a real deployment)</span></div>
        <div className="d-flex flex-wrap gap-2">
          {DEMO.map((d) => (
            <button key={d.username} type="button" className="btn btn-sm btn-outline-secondary"
              onClick={() => setForm({ username: d.username, password: d.password })}>
              {d.role}: <code>{d.username}</code>
            </button>
          ))}
        </div>
      </div>
    </AuthShell>
  );
}
