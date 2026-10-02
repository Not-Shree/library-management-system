import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { EmptyState, ErrorAlert, Loading, PageHeader, Pagination, SearchBox, StatusBadge } from '../../components/Common.jsx';
import { formatDateTime, money } from '../../utils/format.js';

export default function Payments() {
  const [q, setQ] = useState('');
  const [method, setMethod] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const search = useDebounced(q);
  const { data, loading, error, reload } = useApi('/payments', { q: search || undefined, method: method || undefined, from: from || undefined, to: to || undefined, page, size: 10 });
  const pageTotal = (data?.content || []).reduce((s, p) => s + Number(p.paidAmount), 0);

  return (
    <>
      <PageHeader title="Payments" subtitle="Fine payments recorded at the counter (offline / mock — no payment gateway)." />
      <div className="filter-bar mb-3">
        <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Member, title or reference" className="filter-search" />
        <select className="form-select" value={method} onChange={(e) => { setMethod(e.target.value); setPage(0); }} aria-label="Method">
          <option value="">All methods</option><option value="CASH">Cash</option><option value="UPI">UPI</option><option value="CARD">Card</option><option value="OTHER">Other</option>
        </select>
        <input type="date" className="form-control" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0); }} aria-label="From date" />
        <input type="date" className="form-control" value={to} onChange={(e) => { setTo(e.target.value); setPage(0); }} aria-label="To date" />
      </div>
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.content.length === 0 ? <EmptyState icon="receipt" title="No payments found" /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Date</th><th>Member</th><th>Book</th><th>Method</th><th>Reference</th><th className="text-end">Paid</th><th className="text-end">Balance after</th><th>Status</th><th>Recorded by</th></tr></thead>
              <tbody>
                {data?.content.map((p) => (
                  <tr key={p.id}>
                    <td className="text-nowrap">{formatDateTime(p.paymentDate)}</td>
                    <td><Link to={`/members/${p.memberId}`}>{p.memberName}</Link><div className="small text-body-secondary">{p.memberCode}</div></td>
                    <td className="cell-title">{p.bookTitle}</td>
                    <td>{p.paymentMethod}</td>
                    <td className="small">{p.referenceNumber || '—'}</td>
                    <td className="text-end fw-semibold">{money(p.paidAmount)}</td>
                    <td className="text-end">{money(p.remainingAmount)}</td>
                    <td><StatusBadge status={p.paymentStatus} /></td>
                    <td className="small">{p.recordedBy || '—'}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot><tr><td colSpan={5} className="text-end small text-body-secondary">Total on this page</td><td className="text-end fw-semibold">{money(pageTotal)}</td><td colSpan={3} /></tr></tfoot>
            </table>
          </div>
        )}
      </div>
      <Pagination page={data?.page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
    </>
  );
}
