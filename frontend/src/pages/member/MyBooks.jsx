import { useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { useApi } from '../../hooks/useApi.js';
import { useToast } from '../../context/ToastContext.jsx';
import { EmptyState, ErrorAlert, Loading, PageHeader, Pagination } from '../../components/Common.jsx';
import BorrowingTable from '../../components/BorrowingTable.jsx';
import { formatDate } from '../../utils/format.js';

export default function MyBooks() {
  const toast = useToast();
  const [page, setPage] = useState(0);
  const [busyId, setBusyId] = useState(null);
  const current = useApi('/me/borrowings/current');
  const history = useApi('/me/borrowings', { page, size: 10 });
  const settings = useApi('/settings').data;

  const renew = async (b) => {
    setBusyId(b.id);
    try {
      const res = await api.post(`/borrowings/${b.id}/renew`);
      toast.success(`Renewed. "${res.data.bookTitle}" is now due on ${formatDate(res.data.dueDate)}.`);
      current.reload(); history.reload();
    } catch (e) { toast.error(errorMessage(e)); } finally { setBusyId(null); }
  };

  return (
    <>
      <PageHeader title="My borrowed books"
        subtitle={settings ? `You can renew a book up to ${settings.maxRenewals} times (${settings.renewalPeriodDays} days each), unless someone has reserved it.` : undefined} />
      <div className="panel mb-4">
        <h2 className="panel-title">Borrowed now</h2>
        <ErrorAlert message={current.error} onRetry={current.reload} />
        {current.loading && !current.data ? <Loading /> : current.data?.length === 0 ? <EmptyState icon="book" title="Nothing borrowed right now" /> : (
          <BorrowingTable rows={current.data || []} showMember={false} actions={(b) => (
            <button className="btn btn-sm btn-outline-primary" disabled={busyId === b.id || (settings && b.renewalCount >= settings.maxRenewals)} onClick={() => renew(b)}
              title={settings && b.renewalCount >= settings.maxRenewals ? 'Maximum renewals reached' : undefined}>
              {busyId === b.id ? 'Renewing…' : 'Renew'}
            </button>
          )} />
        )}
      </div>
      <div className="panel">
        <h2 className="panel-title">Borrowing history</h2>
        {history.loading && !history.data ? <Loading /> : history.data?.content.length === 0 ? <EmptyState icon="clock-history" title="No history yet" /> : (
          <>
            <BorrowingTable rows={history.data?.content || []} showMember={false} />
            <Pagination page={history.data?.page} totalPages={history.data?.totalPages} totalElements={history.data?.totalElements} onChange={setPage} />
          </>
        )}
      </div>
    </>
  );
}
