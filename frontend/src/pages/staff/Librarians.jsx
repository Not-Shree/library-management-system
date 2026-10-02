import { useState } from 'react';
import api, { errorMessage, fieldErrors } from '../../api/client.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { EmptyState, ErrorAlert, Field, Loading, Modal, PageHeader, StatusBadge } from '../../components/Common.jsx';
import { formatDate } from '../../utils/format.js';

/** Admin: manage librarian and admin accounts. */
export default function Librarians() {
  const toast = useToast();
  const { user } = useAuth();
  const { data, loading, error, reload } = useApi('/users/staff');
  const [form, setForm] = useState(null);     // {} new, {id,...} edit
  const [errors, setErrors] = useState({});
  const [reset, setReset] = useState(null);
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  const save = async () => {
    setBusy(true); setErrors({});
    try {
      if (form.id) await api.put(`/users/staff/${form.id}`, { email: form.email, fullName: form.fullName, enabled: form.enabled });
      else await api.post('/users/staff', { username: form.username, email: form.email, fullName: form.fullName, password: form.password, role: form.role });
      toast.success('Account saved'); setForm(null); reload();
    } catch (e) { setErrors(fieldErrors(e)); toast.error(errorMessage(e)); } finally { setBusy(false); }
  };

  const doReset = async () => {
    setBusy(true);
    try { await api.post(`/users/${reset.id}/reset-password`, { newPassword: password }); toast.success('Password reset'); setReset(null); }
    catch (e) { toast.error(errorMessage(e)); } finally { setBusy(false); }
  };

  const f = (name, props = {}) => <input id={name} className={`form-control ${errors[name] ? 'is-invalid' : ''}`} value={form?.[name] || ''} onChange={(e) => setForm({ ...form, [name]: e.target.value })} {...props} />;

  return (
    <>
      <PageHeader title="Librarians & admins" subtitle="Staff accounts that can use the circulation desk."
        actions={<button className="btn btn-primary" onClick={() => { setErrors({}); setForm({ role: 'LIBRARIAN', enabled: true }); }}><i className="bi bi-person-plus me-1" />Add staff account</button>} />
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.length === 0 ? <EmptyState title="No staff accounts" /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Name</th><th>Username</th><th>Email</th><th>Role</th><th>Status</th><th>Created</th><th /></tr></thead>
              <tbody>{data?.map((u) => (
                <tr key={u.id}>
                  <td className="fw-semibold">{u.fullName}{u.id === user.id && <span className="small text-body-secondary"> (you)</span>}</td>
                  <td><code>{u.username}</code></td><td>{u.email}</td><td><StatusBadge status={u.role} /></td>
                  <td>{u.enabled ? <StatusBadge status="ACTIVE" /> : <StatusBadge status="INACTIVE" label="Disabled" />}</td>
                  <td>{formatDate(u.createdAt)}</td>
                  <td className="text-end text-nowrap">
                    <button className="btn btn-sm btn-outline-secondary me-2" onClick={() => { setErrors({}); setForm({ ...u }); }}>Edit</button>
                    <button className="btn btn-sm btn-outline-secondary" onClick={() => { setPassword(''); setReset(u); }}>Reset password</button>
                  </td>
                </tr>))}</tbody>
            </table>
          </div>
        )}
      </div>

      <Modal show={!!form} title={form?.id ? 'Edit staff account' : 'Add staff account'} onClose={() => setForm(null)}
        footer={<><button className="btn btn-light" onClick={() => setForm(null)}>Cancel</button><button className="btn btn-primary" onClick={save} disabled={busy}>Save</button></>}>
        {form && (
          <div className="row g-3">
            <Field label="Full name" name="fullName" error={errors.fullName} className="col-12">{f('fullName')}</Field>
            <Field label="Email" name="email" error={errors.email} className="col-12">{f('email', { type: 'email' })}</Field>
            {!form.id && <>
              <Field label="Username" name="username" error={errors.username}>{f('username', { autoComplete: 'off' })}</Field>
              <Field label="Role" name="role">
                <select id="role" className="form-select" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                  <option value="LIBRARIAN">Librarian</option><option value="ADMIN">Admin</option>
                </select>
              </Field>
              <Field label="Password" name="password" error={errors.password} className="col-12" hint="8+ characters with a letter and a number">{f('password', { type: 'password', autoComplete: 'new-password' })}</Field>
            </>}
            {form.id && (
              <div className="col-12 form-check form-switch ms-2">
                <input id="enabled" className="form-check-input" type="checkbox" checked={!!form.enabled} disabled={form.id === user.id}
                  onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />
                <label className="form-check-label" htmlFor="enabled">Account enabled</label>
              </div>
            )}
          </div>
        )}
      </Modal>
      <Modal show={!!reset} title={`Reset password for ${reset?.username}`} onClose={() => setReset(null)}
        footer={<><button className="btn btn-light" onClick={() => setReset(null)}>Cancel</button><button className="btn btn-primary" onClick={doReset} disabled={busy || !password}>Reset</button></>}>
        <Field label="New password" name="staffPassword" className="col-12" hint="8+ characters with a letter and a number">
          <input id="staffPassword" type="text" className="form-control" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="off" />
        </Field>
      </Modal>
    </>
  );
}
