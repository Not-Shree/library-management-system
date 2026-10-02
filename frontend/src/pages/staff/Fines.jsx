import { useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { EmptyState, ErrorAlert, Field, Loading, Modal, PageHeader, Pagination, SearchBox, StatusBadge } from '../../components/Common.jsx';
import PaymentModal from '../../components/PaymentModal.jsx';
import { formatDate, money } from '../../utils/format.js';

export default function Fines() {
  const toast = useToast();
  const { isAdmin } = useAuth();
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [paying, setPaying] = useState(null);
  const [waiving, setWaiving] = useState(null);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const search = useDebounced(q);
  const { data, loading, error, reload } = useApi('/fines', { q: search || undefined, status: status || undefined, page, size: 10 });

  const waive = async () => {
    setBusy(true);
    try { await api.post(`/fines/${waiving.id}/waive`, { reason }); toast.success('Fine waived'); setWaiving(null); reload(); }
    catch (e) { toast.error(errorMessage(e)); } finally { setBusy(false); }
  };

  return (
    <>
      <PageHeader title="Fines" subtitle="Fines are created automatically when a book is returned late."
        actions={<Link to="/payments" className="btn btn-outline-primary"><i className="bi bi-receipt me-1" />Payment history</Link>} />
      <div className="filter-bar mb-3">
        <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Member, member ID or title" className="filter-search" />
        <select className="form-select" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} aria-label="Status">
          <option value="">All fines</option><option value="PENDING">Pending</option><option value="PARTIALLY_PAID">Part paid</option>
          <option value="PAID">Paid</option><option value="WAIVED">Waived</option>
        </select>
      </div>
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.content.length === 0 ? <EmptyState icon="cash-coin" title="No fines match" /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Member</th><th>Book</th><th>Due → returned</th><th>Days late</th><th className="text-end">Fine</th><th className="text-end">Paid</th><th className="text-end">Owed</th><th>Status</th><th /></tr></thead>
              <tbody>
                {data?.content.map((f) => (
                  <tr key={f.id}>
                    <td><Link to={`/members/${f.memberId}`} className="fw-semibold">{f.memberName}</Link><div className="small text-body-secondary">{f.memberCode}</div></td>
                    <td className="cell-title">{f.bookTitle}<div className="small"><code>{f.copyCode}</code></div></td>
                    <td className="small text-nowrap">{formatDate(f.dueDate)} → {formatDate(f.returnDate)}</td>
                    <td>{f.overdueDays} <span className="small text-body-secondary">× {money(f.finePerDay)}</span></td>
                    <td className="text-end">{money(f.amount)}</td>
                    <td className="text-end">{money(f.paidAmount)}{Number(f.waivedAmount) > 0 && <div className="small text-body-secondary">waived {money(f.waivedAmount)}</div>}</td>
                    <td className="text-end fw-semibold">{money(f.outstandingAmount)}</td>
                    <td><StatusBadge status={f.status} /></td>
                    <td className="text-end text-nowrap">
                      {Number(f.outstandingAmount) > 0 && <>
                        <button className="btn btn-sm btn-success" onClick={() => setPaying(f)}>Record payment</button>
                        {isAdmin && <button className="btn btn-sm btn-link text-body-secondary" onClick={() => { setReason(''); setWaiving(f); }}>Waive</button>}
                      </>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      <Pagination page={data?.page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      <PaymentModal fine={paying} onClose={() => setPaying(null)} onPaid={() => { setPaying(null); reload(); }} />
      <Modal show={!!waiving} title="Waive fine" onClose={() => setWaiving(null)}
        footer={<><button className="btn btn-light" onClick={() => setWaiving(null)}>Cancel</button>
          <button className="btn btn-warning" disabled={busy || reason.trim().length < 3} onClick={waive}>Waive {money(waiving?.outstandingAmount)}</button></>}>
        <p>The remaining {money(waiving?.outstandingAmount)} owed by {waiving?.memberName} will be written off. This is recorded in the audit log.</p>
        <Field label="Reason" name="waiveReason" className="col-12">
          <input id="waiveReason" className="form-control" value={reason} onChange={(e) => setReason(e.target.value)} placeholder="e.g. Medical leave, approved by HOD" />
        </Field>
      </Modal>
    </>
  );
}
