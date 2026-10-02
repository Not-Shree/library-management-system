import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { useToast } from '../../context/ToastContext.jsx';
import { EmptyState, ErrorAlert, Loading, PageHeader, Pagination, SearchBox } from '../../components/Common.jsx';
import BorrowingTable from '../../components/BorrowingTable.jsx';
import { Link } from 'react-router-dom';

export default function Borrowings() {
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const view = params.get('overdue') === 'true' ? 'OVERDUE' : params.get('status') || '';
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const search = useDebounced(q);
  const query = { q: search || undefined, page, size: 10, sort: view === 'OVERDUE' ? 'due' : 'issued', dir: view === 'OVERDUE' ? 'asc' : 'desc' };
  if (view === 'OVERDUE') query.overdue = true; else if (view) query.status = view;
  const { data, loading, error, reload } = useApi('/borrowings', query);

  const setView = (v) => {
    setPage(0);
    if (v === 'OVERDUE') setParams({ overdue: 'true' }); else if (v) setParams({ status: v }); else setParams({});
  };

  const renew = async (b) => {
    try {
      const res = await api.post(`/borrowings/${b.id}/renew`);
      toast.success(`Renewed. New due date ${res.data.dueDate}.`);
      reload();
    } catch (e) { toast.error(errorMessage(e)); }
  };

  return (
    <>
      <PageHeader title="Borrowings" subtitle="Every loan, current and past." />
      <div className="filter-bar mb-3">
        <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Member, member ID, title or copy code" className="filter-search" />
        <div className="btn-group" role="group" aria-label="Filter by status">
          {[['', 'All'], ['BORROWED', 'Borrowed'], ['OVERDUE', 'Overdue'], ['RETURNED', 'Returned']].map(([v, label]) => (
            <button key={v} className={`btn ${view === v ? 'btn-primary' : 'btn-outline-primary'}`} onClick={() => setView(v)}>{label}</button>
          ))}
        </div>
      </div>
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.content.length === 0 ? <EmptyState icon="journal" title="No borrowings match" /> : (
          <BorrowingTable rows={data?.content || []} actions={(b) => b.status === 'BORROWED' && (
            <div className="d-flex gap-1 justify-content-end">
              <Link className="btn btn-sm btn-outline-primary" to={`/return?borrowing=${b.id}`}>Return</Link>
              <button className="btn btn-sm btn-outline-secondary" onClick={() => renew(b)}>Renew</button>
            </div>
          )} />
        )}
      </div>
      <Pagination page={data?.page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
    </>
  );
}
