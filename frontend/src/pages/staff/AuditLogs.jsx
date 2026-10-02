import { useState } from 'react';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { EmptyState, ErrorAlert, Loading, PageHeader, Pagination, SearchBox } from '../../components/Common.jsx';
import { formatDateTime } from '../../utils/format.js';

const ENTITIES = ['Book', 'BookCopy', 'Member', 'Borrowing', 'Fine', 'Reservation', 'User', 'LibrarySettings', 'Author', 'Publisher', 'Category'];

export default function AuditLogs() {
  const [q, setQ] = useState('');
  const [entityType, setEntityType] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const search = useDebounced(q);
  const { data, loading, error, reload } = useApi('/audit-logs', { q: search || undefined, entityType: entityType || undefined, from: from || undefined, to: to || undefined, page, size: 20 });

  return (
    <>
      <PageHeader title="Audit logs" subtitle="Who changed what, and when. Entries cannot be edited." />
      <div className="filter-bar mb-3">
        <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="User, action or details" className="filter-search" />
        <select className="form-select" value={entityType} onChange={(e) => { setEntityType(e.target.value); setPage(0); }} aria-label="Record type">
          <option value="">All record types</option>{ENTITIES.map((e) => <option key={e} value={e}>{e}</option>)}
        </select>
        <input type="date" className="form-control" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0); }} aria-label="From date" />
        <input type="date" className="form-control" value={to} onChange={(e) => { setTo(e.target.value); setPage(0); }} aria-label="To date" />
      </div>
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.content.length === 0 ? <EmptyState icon="shield-check" title="No log entries" /> : (
          <div className="table-responsive">
            <table className="table table-sm align-middle mb-0">
              <thead><tr><th>When</th><th>User</th><th>Action</th><th>Record</th><th>Details</th></tr></thead>
              <tbody>{data?.content.map((a) => (
                <tr key={a.id}>
                  <td className="text-nowrap small">{formatDateTime(a.createdAt)}</td>
                  <td><code>{a.username}</code></td>
                  <td><span className="badge text-bg-light border">{a.action}</span></td>
                  <td className="small text-nowrap">{a.entityType}{a.entityId ? ` #${a.entityId}` : ''}</td>
                  <td className="small">{a.details || '—'}</td>
                </tr>))}</tbody>
            </table>
          </div>
        )}
      </div>
      <Pagination page={data?.page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
    </>
  );
}
