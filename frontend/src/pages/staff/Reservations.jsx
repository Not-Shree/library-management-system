import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { useToast } from '../../context/ToastContext.jsx';
import { ConfirmDialog, EmptyState, ErrorAlert, Loading, Modal, PageHeader, Pagination, ReservationBadge, SearchBox } from '../../components/Common.jsx';
import MemberPicker from '../../components/MemberPicker.jsx';
import { formatDate, formatDateTime } from '../../utils/format.js';

const STATUSES = [['', 'All'], ['WAITING', 'Waiting'], ['AVAILABLE', 'Ready to collect'], ['FULFILLED', 'Fulfilled'], ['CANCELLED', 'Cancelled'], ['EXPIRED', 'Expired']];

export default function Reservations() {
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const status = params.get('status') || '';
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const [member, setMember] = useState(null);
  const [book, setBook] = useState(null);
  const [bookQ, setBookQ] = useState('');
  const [cancelling, setCancelling] = useState(null);
  const [busy, setBusy] = useState(false);
  const search = useDebounced(q);
  const bookSearch = useDebounced(bookQ, 300);
  const { data, loading, error, reload } = useApi('/reservations', { q: search || undefined, status: status || undefined, page, size: 10 });
  const books = useApi('/books', { q: bookSearch, size: 6, available: false }, { enabled: creating && !book && bookSearch.trim().length >= 2 });

  // Opened from the issue page: /reservations?book=5&member=3
  useEffect(() => {
    const b = params.get('book'); const m = params.get('member');
    if (b || m) {
      setCreating(true);
      if (b) api.get(`/books/${b}`).then((r) => setBook(r.data)).catch(() => {});
      if (m) api.get(`/members/${m}`).then((r) => setMember(r.data)).catch(() => {});
    }
  }, [params]);

  const closeCreate = () => { setCreating(false); setMember(null); setBook(null); setBookQ(''); if (params.get('book') || params.get('member')) setParams({}); };

  const create = async () => {
    setBusy(true);
    try {
      const res = await api.post('/reservations', { bookId: book.id, memberId: member.id });
      toast.success(`${res.data.memberName} is #${res.data.queuePosition} in the queue for "${res.data.bookTitle}"`);
      closeCreate(); reload();
    } catch (e) { toast.error(errorMessage(e)); } finally { setBusy(false); }
  };

  const cancel = async () => {
    setBusy(true);
    try { await api.delete(`/reservations/${cancelling.id}`); toast.success('Reservation cancelled'); reload(); }
    catch (e) { toast.error(errorMessage(e)); } finally { setBusy(false); setCancelling(null); }
  };

  return (
    <>
      <PageHeader title="Reservations" subtitle="First come, first served. A returned copy is held for the next member in the queue."
        actions={<button className="btn btn-primary" onClick={() => setCreating(true)}><i className="bi bi-bookmark-plus me-1" />New reservation</button>} />
      <div className="filter-bar mb-3">
        <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Member or book title" className="filter-search" />
        <select className="form-select" value={status} onChange={(e) => { setPage(0); setParams(e.target.value ? { status: e.target.value } : {}); }} aria-label="Status">
          {STATUSES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
        </select>
      </div>
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.content.length === 0 ? <EmptyState icon="bookmark" title="No reservations" /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Book</th><th>Member</th><th>Reserved</th><th>Status</th><th>Queue</th><th>Held copy</th><th>Collect by</th><th /></tr></thead>
              <tbody>
                {data?.content.map((r) => (
                  <tr key={r.id}>
                    <td className="cell-title"><Link to={`/books/${r.bookId}`}>{r.bookTitle}</Link></td>
                    <td><Link to={`/members/${r.memberId}`}>{r.memberName}</Link><div className="small text-body-secondary">{r.memberCode}</div></td>
                    <td className="small text-nowrap">{formatDateTime(r.reservedAt)}</td>
                    <td><ReservationBadge status={r.status} /></td>
                    <td>{r.queuePosition ? `#${r.queuePosition}` : '—'}</td>
                    <td>{r.heldCopyCode ? <code>{r.heldCopyCode}</code> : '—'}</td>
                    <td>{formatDate(r.expiryDate)}</td>
                    <td className="text-end text-nowrap">
                      {r.status === 'AVAILABLE' && <Link className="btn btn-sm btn-primary me-1" to={`/issue?member=${r.memberId}&book=${r.bookId}`}>Issue</Link>}
                      {(r.status === 'WAITING' || r.status === 'AVAILABLE') && <button className="btn btn-sm btn-outline-danger" onClick={() => setCancelling(r)}>Cancel</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      <Pagination page={data?.page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />

      <Modal show={creating} title="Reserve a book for a member" onClose={closeCreate} size="lg"
        footer={<><button className="btn btn-light" onClick={closeCreate}>Cancel</button>
          <button className="btn btn-primary" disabled={!member || !book || busy} onClick={create}>Add to queue</button></>}>
        <label className="form-label">Member</label>
        <MemberPicker value={member} onChange={setMember} />
        <label className="form-label mt-3" htmlFor="resBook">Book (only titles with no free copy can be reserved)</label>
        {book ? (
          <div className="picked-card d-flex justify-content-between align-items-center">
            <div><div className="fw-semibold">{book.title}</div><div className="small text-body-secondary">{book.availableCopies} of {book.totalCopies} on the shelf · {book.activeReservations} already waiting</div></div>
            <button className="btn btn-sm btn-link" onClick={() => setBook(null)}>Change</button>
          </div>
        ) : (
          <div className="position-relative">
            <input id="resBook" className="form-control" value={bookQ} onChange={(e) => setBookQ(e.target.value)} placeholder="Search title or ISBN" />
            {bookSearch.trim().length >= 2 && (
              <div className="list-group picker-results shadow-sm">
                {books.data?.content.length === 0 && <div className="list-group-item small text-body-secondary">No unavailable book matches — copies may be on the shelf</div>}
                {books.data?.content.map((b) => (
                  <button key={b.id} type="button" className="list-group-item list-group-item-action" onClick={() => setBook(b)}>
                    <div className="fw-semibold">{b.title}</div><div className="small text-body-secondary">{b.authorName} · {b.activeReservations} waiting</div>
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
      </Modal>
      <ConfirmDialog show={!!cancelling} busy={busy} title="Cancel reservation?" confirmLabel="Cancel reservation"
        message={`${cancelling?.memberName} will leave the queue for "${cancelling?.bookTitle}".${cancelling?.status === 'AVAILABLE' ? ' The held copy goes to the next member, or back on the shelf.' : ''}`}
        onConfirm={cancel} onCancel={() => setCancelling(null)} />
    </>
  );
}
