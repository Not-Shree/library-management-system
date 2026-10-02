import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { ConfirmDialog, EmptyState, ErrorAlert, Field, Loading, Modal, PageHeader, Pagination, ReservationBadge, StatusBadge } from '../../components/Common.jsx';
import BorrowingTable from '../../components/BorrowingTable.jsx';
import PaymentModal from '../../components/PaymentModal.jsx';
import { formatDate, formatDateTime, money } from '../../utils/format.js';
import MemberForm from './MemberForm.jsx';

const TABS = [['history', 'Borrowing history'], ['fines', 'Fines'], ['payments', 'Payments'], ['reservations', 'Reservations']];

export default function MemberDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const { isAdmin } = useAuth();
  const [tab, setTab] = useState('history');
  const [page, setPage] = useState(0);
  const member = useApi(`/members/${id}`);
  const history = useApi(`/members/${id}/borrowings`, { page, size: 8 }, { enabled: tab === 'history' });
  const fines = useApi(`/members/${id}/fines`, {}, { enabled: tab === 'fines' });
  const payments = useApi(`/members/${id}/payments`, {}, { enabled: tab === 'payments' });
  const reservations = useApi(`/members/${id}/reservations`, {}, { enabled: tab === 'reservations' });
  const [editing, setEditing] = useState(false);
  const [paying, setPaying] = useState(null);
  const [confirm, setConfirm] = useState(null);   // 'status' | 'delete'
  const [loginOpen, setLoginOpen] = useState(false);
  const [resetOpen, setResetOpen] = useState(false);
  const [creds, setCreds] = useState({ username: '', password: '' });
  const [busy, setBusy] = useState(false);

  if (member.loading && !member.data) return <Loading />;
  if (member.error) return <ErrorAlert message={member.error} />;
  const m = member.data;
  const active = m.status === 'ACTIVE';

  const run = async (fn, message, after) => {
    setBusy(true);
    try { await fn(); toast.success(message); after?.(); member.reload(); } catch (e) { toast.error(errorMessage(e)); } finally { setBusy(false); }
  };

  const toggleStatus = () => run(() => api.patch(`/members/${id}/status`, { status: active ? 'INACTIVE' : 'ACTIVE' }),
    active ? 'Member deactivated' : 'Member activated', () => setConfirm(null));
  const remove = async () => {
    setBusy(true);
    try { await api.delete(`/members/${id}`); toast.success('Member deleted'); navigate('/members'); }
    catch (e) { toast.error(errorMessage(e)); setConfirm(null); } finally { setBusy(false); }
  };
  const createLogin = () => run(() => api.post(`/members/${id}/login`, creds), 'Login created', () => setLoginOpen(false));
  const resetPassword = () => run(() => api.post(`/users/${m.userId}/reset-password`, { newPassword: creds.password }),
    'Password reset. Share the temporary password with the member.', () => setResetOpen(false));

  return (
    <>
      <nav aria-label="breadcrumb"><ol className="breadcrumb small"><li className="breadcrumb-item"><Link to="/members">Members</Link></li><li className="breadcrumb-item active">{m.fullName}</li></ol></nav>
      <PageHeader title={m.fullName} subtitle={`${m.memberCode} · member since ${formatDate(m.membershipDate)}`}
        actions={<>
          <Link to={`/issue?member=${m.id}`} className={`btn btn-primary ${active ? '' : 'disabled'}`}><i className="bi bi-box-arrow-up-right me-1" />Issue book</Link>
          <button className="btn btn-outline-primary" onClick={() => setEditing(true)}>Edit</button>
          <button className={`btn btn-outline-${active ? 'warning' : 'success'}`} onClick={() => setConfirm('status')}>{active ? 'Deactivate' : 'Activate'}</button>
          {isAdmin && <button className="btn btn-outline-danger" onClick={() => setConfirm('delete')}>Delete</button>}
        </>} />

      <div className="row g-3 mb-4">
        <div className="col-lg-8">
          <div className="panel h-100">
            <dl className="detail-list mb-0">
              <dt>Status</dt><dd><StatusBadge status={m.status} /></dd>
              <dt>Email</dt><dd>{m.email}</dd>
              <dt>Phone</dt><dd>{m.phone || '—'}</dd>
              <dt>Department</dt><dd>{m.department || '—'}</dd>
              <dt>Course / year</dt><dd>{m.course || '—'}{m.yearOfStudy ? `, year ${m.yearOfStudy}` : ''}</dd>
              <dt>Address</dt><dd>{m.address || '—'}</dd>
              <dt>Portal login</dt>
              <dd>
                {m.username ? <><code>{m.username}</code> <button className="btn btn-sm btn-link" onClick={() => { setCreds({ username: '', password: '' }); setResetOpen(true); }}>Reset password</button></>
                  : <button className="btn btn-sm btn-outline-secondary" onClick={() => { setCreds({ username: '', password: '' }); setLoginOpen(true); }}>Create login</button>}
              </dd>
            </dl>
          </div>
        </div>
        <div className="col-lg-4 d-flex flex-column gap-3">
          <div className="stat-card"><span className="stat-label">Books borrowed</span>
            <div className="stat-value">{m.currentBorrowedBooks}<small className="fs-6 text-body-secondary"> / {m.maxBooksAllowed}</small></div>
            <div className="stat-note">{m.customLimit ? 'Custom limit for this member' : 'Library default limit'}</div></div>
          <div className={`stat-card ${Number(m.outstandingFine) > 0 ? 'stat-bad' : 'stat-good'}`}><span className="stat-label">Fines owed</span>
            <div className="stat-value">{money(m.outstandingFine)}</div></div>
        </div>
      </div>

      <ul className="nav nav-tabs mb-3" role="tablist">
        {TABS.map(([key, label]) => (
          <li className="nav-item" key={key}>
            <button className={`nav-link ${tab === key ? 'active' : ''}`} role="tab" aria-selected={tab === key} onClick={() => setTab(key)}>{label}</button>
          </li>
        ))}
      </ul>

      <div className="panel">
        {tab === 'history' && (history.loading && !history.data ? <Loading /> : history.data?.content.length === 0
          ? <EmptyState icon="journal" title="No borrowings yet" />
          : <><BorrowingTable rows={history.data?.content || []} showMember={false} />
            <Pagination page={history.data?.page} totalPages={history.data?.totalPages} totalElements={history.data?.totalElements} onChange={setPage} /></>)}

        {tab === 'fines' && (fines.loading && !fines.data ? <Loading /> : fines.data?.length === 0 ? <EmptyState icon="emoji-smile" title="No fines" /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Book</th><th>Due</th><th>Returned</th><th>Days late</th><th className="text-end">Fine</th><th className="text-end">Paid</th><th className="text-end">Owed</th><th>Status</th><th /></tr></thead>
              <tbody>{fines.data?.map((f) => (
                <tr key={f.id}>
                  <td className="cell-title">{f.bookTitle}</td><td>{formatDate(f.dueDate)}</td><td>{formatDate(f.returnDate)}</td><td>{f.overdueDays}</td>
                  <td className="text-end">{money(f.amount)}</td><td className="text-end">{money(f.paidAmount)}</td>
                  <td className="text-end fw-semibold">{money(f.outstandingAmount)}</td><td><StatusBadge status={f.status} /></td>
                  <td className="text-end">{Number(f.outstandingAmount) > 0 && <button className="btn btn-sm btn-success" onClick={() => setPaying(f)}>Record payment</button>}</td>
                </tr>))}</tbody>
            </table>
          </div>))}

        {tab === 'payments' && (payments.loading && !payments.data ? <Loading /> : payments.data?.length === 0 ? <EmptyState icon="receipt" title="No payments recorded" /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Date</th><th>Book</th><th>Method</th><th>Reference</th><th className="text-end">Paid</th><th className="text-end">Balance after</th><th>Status</th><th>Recorded by</th></tr></thead>
              <tbody>{payments.data?.map((p) => (
                <tr key={p.id}><td>{formatDateTime(p.paymentDate)}</td><td className="cell-title">{p.bookTitle}</td><td>{p.paymentMethod}</td><td>{p.referenceNumber || '—'}</td>
                  <td className="text-end fw-semibold">{money(p.paidAmount)}</td><td className="text-end">{money(p.remainingAmount)}</td><td><StatusBadge status={p.paymentStatus} /></td><td>{p.recordedBy || '—'}</td></tr>))}</tbody>
            </table>
          </div>))}

        {tab === 'reservations' && (reservations.loading && !reservations.data ? <Loading /> : reservations.data?.length === 0 ? <EmptyState icon="bookmark" title="No reservations" /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Book</th><th>Reserved</th><th>Status</th><th>Queue</th><th>Held copy</th><th>Collect by</th></tr></thead>
              <tbody>{reservations.data?.map((r) => (
                <tr key={r.id}><td className="cell-title">{r.bookTitle}</td><td>{formatDateTime(r.reservedAt)}</td><td><ReservationBadge status={r.status} /></td>
                  <td>{r.queuePosition ? `#${r.queuePosition}` : '—'}</td><td>{r.heldCopyCode ? <code>{r.heldCopyCode}</code> : '—'}</td><td>{formatDate(r.expiryDate)}</td></tr>))}</tbody>
            </table>
          </div>))}
      </div>

      <MemberForm show={editing} member={m} onClose={() => setEditing(false)} onSaved={() => { setEditing(false); member.reload(); }} />
      <PaymentModal fine={paying} onClose={() => setPaying(null)} onPaid={() => { setPaying(null); fines.reload(); member.reload(); }} />
      <ConfirmDialog show={confirm === 'status'} busy={busy} variant={active ? 'warning' : 'success'}
        title={active ? 'Deactivate member?' : 'Activate member?'} confirmLabel={active ? 'Deactivate' : 'Activate'}
        message={active ? `${m.fullName} will not be able to borrow or reserve books, and their login will be disabled.` : `${m.fullName} will be able to borrow books again.`}
        onConfirm={toggleStatus} onCancel={() => setConfirm(null)} />
      <ConfirmDialog show={confirm === 'delete'} busy={busy} title="Delete member?" confirmLabel="Delete"
        message="Members with borrowing history cannot be deleted (their records are kept). Deactivate them instead." onConfirm={remove} onCancel={() => setConfirm(null)} />
      <Modal show={loginOpen} title="Create portal login" onClose={() => setLoginOpen(false)}
        footer={<><button className="btn btn-light" onClick={() => setLoginOpen(false)}>Cancel</button><button className="btn btn-primary" disabled={busy || !creds.username || !creds.password} onClick={createLogin}>Create login</button></>}>
        <div className="row g-3">
          <Field label="Username" name="newUsername" className="col-12"><input id="newUsername" className="form-control" value={creds.username} onChange={(e) => setCreds({ ...creds, username: e.target.value })} /></Field>
          <Field label="Temporary password" name="newPassword" className="col-12" hint="8+ characters with a letter and a number"><input id="newPassword" type="password" className="form-control" value={creds.password} onChange={(e) => setCreds({ ...creds, password: e.target.value })} /></Field>
        </div>
      </Modal>
      <Modal show={resetOpen} title={`Reset password for ${m.username}`} onClose={() => setResetOpen(false)}
        footer={<><button className="btn btn-light" onClick={() => setResetOpen(false)}>Cancel</button><button className="btn btn-primary" disabled={busy || !creds.password} onClick={resetPassword}>Reset password</button></>}>
        <Field label="New temporary password" name="resetPassword" className="col-12" hint="The member should change it from their Profile after logging in">
          <input id="resetPassword" type="text" className="form-control" value={creds.password} onChange={(e) => setCreds({ ...creds, password: e.target.value })} autoComplete="off" />
        </Field>
      </Modal>
    </>
  );
}
