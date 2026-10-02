import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { DueStamp, EmptyState, ErrorAlert, Loading, PageHeader, Pagination, SearchBox, StatusBadge } from '../../components/Common.jsx';
import { formatDate } from '../../utils/format.js';

const STATUSES = ['AVAILABLE', 'BORROWED', 'RESERVED', 'DAMAGED', 'LOST', 'WITHDRAWN'];

/** Every physical copy in the library. Type or scan a copy code to find it. */
export default function Copies() {
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const search = useDebounced(q);
  const { data, loading, error, reload } = useApi('/copies', { q: search || undefined, status: status || undefined, page, size: 15 });

  return (
    <>
      <PageHeader title="Book copies" subtitle="Each physical item has its own code. Add copies from a book's page." />
      <div className="filter-bar mb-3">
        <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Copy code, title or ISBN" className="filter-search" />
        <select className="form-select" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} aria-label="Status">
          <option value="">All statuses</option>{STATUSES.map((s) => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
        </select>
      </div>
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.content.length === 0 ? <EmptyState icon="upc-scan" title="No copies match" /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Copy code</th><th>Book</th><th>Status</th><th>With</th><th>Due</th><th>Acquired</th></tr></thead>
              <tbody>
                {data?.content.map((c) => (
                  <tr key={c.id}>
                    <td><code className="fs-6">{c.copyCode}</code></td>
                    <td><Link to={`/books/${c.bookId}`}>{c.bookTitle}</Link></td>
                    <td><StatusBadge status={c.status} /></td>
                    <td>{c.currentBorrower || '—'}</td>
                    <td>{c.dueDate ? <DueStamp date={c.dueDate} /> : '—'}</td>
                    <td>{formatDate(c.acquiredDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      <Pagination page={data?.page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
    </>
  );
}
