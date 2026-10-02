import { useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { useApi } from '../../hooks/useApi.js';
import { useToast } from '../../context/ToastContext.jsx';
import { ConfirmDialog, EmptyState, ErrorAlert, Loading, PageHeader, ReservationBadge } from '../../components/Common.jsx';
import { formatDate, formatDateTime } from '../../utils/format.js';

export default function MyReservations() {
  const toast = useToast();
  const { data, loading, error, reload } = useApi('/me/reservations');
  const [cancelling, setCancelling] = useState(null);
  const [busy, setBusy] = useState(false);

  const cancel = async () => {
    setBusy(true);
    try { await api.delete(`/reservations/${cancelling.id}`); toast.success('Reservation cancelled'); reload(); }
    catch (e) { toast.error(errorMessage(e)); } finally { setBusy(false); setCancelling(null); }
  };

  return (
    <>
      <PageHeader title="My reservations" subtitle="When a copy comes back it is held for you, in the order people reserved."
        actions={<Link to="/browse" className="btn btn-outline-primary">Reserve another book</Link>} />
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.length === 0 ? (
          <EmptyState icon="bookmark" title="No reservations">Books that are all borrowed show a “Reserve book” button in the catalogue.</EmptyState>
        ) : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Book</th><th>Reserved on</th><th>Status</th><th>Your place</th><th>Collect</th><th /></tr></thead>
              <tbody>{data?.map((r) => (
                <tr key={r.id}>
                  <td className="cell-title fw-semibold">{r.bookTitle}</td>
                  <td className="small">{formatDateTime(r.reservedAt)}</td>
                  <td><ReservationBadge status={r.status} /></td>
                  <td>{r.queuePosition ? `#${r.queuePosition} in queue` : '—'}</td>
                  <td>{r.status === 'AVAILABLE' ? <span>Copy <code>{r.heldCopyCode}</code> by <strong>{formatDate(r.expiryDate)}</strong></span> : '—'}</td>
                  <td className="text-end">{(r.status === 'WAITING' || r.status === 'AVAILABLE') && <button className="btn btn-sm btn-outline-danger" onClick={() => setCancelling(r)}>Cancel</button>}</td>
                </tr>))}</tbody>
            </table>
          </div>
        )}
      </div>
      <ConfirmDialog show={!!cancelling} busy={busy} title="Cancel reservation?" confirmLabel="Cancel reservation"
        message={`You will leave the queue for "${cancelling?.bookTitle}".`} onConfirm={cancel} onCancel={() => setCancelling(null)} />
    </>
  );
}
