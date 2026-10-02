import { useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { useApi } from '../../hooks/useApi.js';
import { useToast } from '../../context/ToastContext.jsx';
import { EmptyState, ErrorAlert, Loading, PageHeader, Pagination } from '../../components/Common.jsx';
import { formatDateTime } from '../../utils/format.js';

const ICON = {
  DUE_SOON: 'calendar-event', OVERDUE: 'alarm', BOOK_ISSUED: 'box-arrow-up-right', BOOK_RETURNED: 'box-arrow-in-down-left',
  BOOK_RENEWED: 'arrow-repeat', FINE_GENERATED: 'cash-coin', RESERVATION_AVAILABLE: 'bookmark-check', RESERVATION_EXPIRED: 'bookmark-x',
};

export default function Notifications() {
  const toast = useToast();
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useApi('/me/notifications', { page, size: 15 });

  const markRead = async (n) => {
    if (n.read) return;
    try { await api.patch(`/me/notifications/${n.id}/read`); reload(); } catch (e) { toast.error(errorMessage(e)); }
  };
  const markAll = async () => {
    try { await api.patch('/me/notifications/read-all'); toast.success('All marked as read'); reload(); } catch (e) { toast.error(errorMessage(e)); }
  };

  return (
    <>
      <PageHeader title="Notifications" subtitle="Reminders and updates from the library."
        actions={<button className="btn btn-outline-primary" onClick={markAll}><i className="bi bi-check2-all me-1" />Mark all as read</button>} />
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.content.length === 0 ? <EmptyState icon="bell" title="No notifications" /> : (
          <ul className="list-group list-group-flush">
            {data?.content.map((n) => (
              <li key={n.id} className={`list-group-item notification ${n.read ? '' : 'unread'}`}>
                <button className="btn btn-link text-reset text-decoration-none d-flex gap-3 w-100 text-start p-0" onClick={() => markRead(n)}>
                  <i className={`bi bi-${ICON[n.type] || 'bell'} notif-icon notif-${n.type}`} aria-hidden="true" />
                  <span className="flex-grow-1">
                    <span className="d-flex justify-content-between gap-2">
                      <span className="fw-semibold">{n.title}{!n.read && <span className="visually-hidden"> (unread)</span>}</span>
                      <span className="small text-body-secondary text-nowrap">{formatDateTime(n.createdAt)}</span>
                    </span>
                    <span className="d-block small">{n.message}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
      <Pagination page={data?.page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
    </>
  );
}
