import { useEffect, useState } from 'react';
import api, { errorMessage, fieldErrors } from '../../api/client.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { Field, PageHeader, StatusBadge } from '../../components/Common.jsx';
import { formatDate } from '../../utils/format.js';

/** Profile for every role: account details and password change; members can also edit phone and address. */
export default function Profile() {
  const { user, isMember } = useAuth();
  const toast = useToast();
  const me = useApi('/me', {}, { enabled: isMember });
  const [contact, setContact] = useState({ phone: '', address: '' });
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState('');

  useEffect(() => { if (me.data) setContact({ phone: me.data.phone || '', address: me.data.address || '' }); }, [me.data]);

  const saveContact = async (e) => {
    e.preventDefault(); setBusy('contact');
    try { await api.put('/me', { phone: contact.phone || null, address: contact.address || null }); toast.success('Profile updated'); me.reload(); }
    catch (err) { toast.error(errorMessage(err)); } finally { setBusy(''); }
  };

  const changePassword = async (e) => {
    e.preventDefault(); setErrors({});
    if (pw.newPassword !== pw.confirm) { setErrors({ confirm: 'Passwords do not match' }); return; }
    setBusy('pw');
    try {
      await api.post('/auth/change-password', { currentPassword: pw.currentPassword, newPassword: pw.newPassword });
      toast.success('Password changed');
      setPw({ currentPassword: '', newPassword: '', confirm: '' });
    } catch (err) { setErrors(fieldErrors(err)); toast.error(errorMessage(err)); } finally { setBusy(''); }
  };

  const m = me.data;
  return (
    <>
      <PageHeader title="Profile" />
      <div className="row g-3">
        <div className="col-lg-6">
          <div className="panel mb-3">
            <h2 className="panel-title">Account</h2>
            <dl className="detail-list mb-0">
              <dt>Name</dt><dd>{user.fullName}</dd>
              <dt>Username</dt><dd><code>{user.username}</code></dd>
              <dt>Email</dt><dd>{user.email}</dd>
              <dt>Role</dt><dd><StatusBadge status={user.role} /></dd>
              {m && <>
                <dt>Member ID</dt><dd>{m.memberCode}</dd>
                <dt>Department</dt><dd>{m.department || '—'}{m.course ? ` · ${m.course}` : ''}{m.yearOfStudy ? `, year ${m.yearOfStudy}` : ''}</dd>
                <dt>Member since</dt><dd>{formatDate(m.membershipDate)}</dd>
                <dt>Borrowing limit</dt><dd>{m.maxBooksAllowed} books</dd>
              </>}
            </dl>
            {m && <p className="small text-body-secondary mt-3 mb-0">To change your name, ID or email, ask the librarian.</p>}
          </div>
          {isMember && (
            <form className="panel" onSubmit={saveContact}>
              <h2 className="panel-title">Contact details</h2>
              <div className="row g-3">
                <Field label="Phone" name="phone" className="col-12"><input id="phone" type="tel" className="form-control" value={contact.phone} onChange={(e) => setContact({ ...contact, phone: e.target.value })} /></Field>
                <Field label="Address" name="address" className="col-12"><input id="address" className="form-control" value={contact.address} onChange={(e) => setContact({ ...contact, address: e.target.value })} /></Field>
                <div className="col-12"><button className="btn btn-primary" disabled={busy === 'contact'}>Save contact details</button></div>
              </div>
            </form>
          )}
        </div>
        <div className="col-lg-6">
          <form className="panel" onSubmit={changePassword}>
            <h2 className="panel-title">Change password</h2>
            <div className="row g-3">
              <Field label="Current password" name="currentPassword" error={errors.currentPassword} className="col-12">
                <input id="currentPassword" type="password" autoComplete="current-password" className="form-control" value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} />
              </Field>
              <Field label="New password" name="newPassword" error={errors.newPassword} className="col-12" hint="8+ characters with a letter and a number">
                <input id="newPassword" type="password" autoComplete="new-password" className={`form-control ${errors.newPassword ? 'is-invalid' : ''}`} value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} />
              </Field>
              <Field label="Repeat new password" name="confirm" error={errors.confirm} className="col-12">
                <input id="confirm" type="password" autoComplete="new-password" className={`form-control ${errors.confirm ? 'is-invalid' : ''}`} value={pw.confirm} onChange={(e) => setPw({ ...pw, confirm: e.target.value })} />
              </Field>
              <div className="col-12"><button className="btn btn-primary" disabled={busy === 'pw' || !pw.currentPassword || !pw.newPassword}>Change password</button></div>
            </div>
          </form>
        </div>
      </div>
    </>
  );
}
