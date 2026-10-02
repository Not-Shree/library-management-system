import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { EmptyState, ErrorAlert, Loading, PageHeader, Pagination, SearchBox, StatusBadge } from '../../components/Common.jsx';
import { money } from '../../utils/format.js';
import MemberForm from './MemberForm.jsx';

export default function Members() {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [adding, setAdding] = useState(false);
  const search = useDebounced(q);
  const { data, loading, error, reload } = useApi('/members', { q: search || undefined, status: status || undefined, page, size: 10 });

  return (
    <>
      <PageHeader title="Members" subtitle="Students and staff who can borrow books."
        actions={<button className="btn btn-primary" onClick={() => setAdding(true)}><i className="bi bi-person-plus me-1" />Add member</button>} />
      <div className="filter-bar mb-3">
        <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Name, member ID, email, phone" className="filter-search" />
        <select className="form-select" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} aria-label="Status">
          <option value="">All members</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option>
        </select>
      </div>
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.content.length === 0 ? <EmptyState icon="people" title="No members found" /> : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead><tr><th>Member</th><th>Department</th><th>Contact</th><th>Books out</th><th className="text-end">Fines owed</th><th>Status</th></tr></thead>
              <tbody>
                {data?.content.map((m) => (
                  <tr key={m.id} className="row-link" onClick={() => navigate(`/members/${m.id}`)}>
                    <td><Link to={`/members/${m.id}`} className="fw-semibold" onClick={(e) => e.stopPropagation()}>{m.fullName}</Link>
                      <div className="small text-body-secondary">{m.memberCode}</div></td>
                    <td>{m.department || '—'}{m.course ? <div className="small text-body-secondary">{m.course}{m.yearOfStudy ? `, year ${m.yearOfStudy}` : ''}</div> : null}</td>
                    <td className="small">{m.email}<div className="text-body-secondary">{m.phone || ''}</div></td>
                    <td>{m.currentBorrowedBooks} / {m.maxBooksAllowed}</td>
                    <td className={`text-end ${Number(m.outstandingFine) > 0 ? 'text-danger fw-semibold' : ''}`}>{money(m.outstandingFine)}</td>
                    <td><StatusBadge status={m.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      <Pagination page={data?.page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      <MemberForm show={adding} onClose={() => setAdding(false)} onSaved={(m) => { setAdding(false); navigate(`/members/${m.id}`); }} />
    </>
  );
}
