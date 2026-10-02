import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { useApi } from '../../hooks/useApi.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { BookCover, ConfirmDialog, DueStamp, EmptyState, ErrorAlert, Field, Loading, Modal, PageHeader, StatusBadge } from '../../components/Common.jsx';
import { formatDate, todayIso } from '../../utils/format.js';
import BookForm from './BookForm.jsx';

const MANUAL = ['AVAILABLE', 'DAMAGED', 'LOST', 'WITHDRAWN'];

export default function BookDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const { isAdmin } = useAuth();
  const book = useApi(`/books/${id}`);
  const copies = useApi(`/books/${id}/copies`);
  const [editing, setEditing] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const [addForm, setAddForm] = useState({ copyCode: '', quantity: 1, acquiredDate: todayIso(), notes: '' });
  const [statusCopy, setStatusCopy] = useState(null);
  const [busy, setBusy] = useState(false);

  const reloadAll = () => { book.reload(); copies.reload(); };

  const addCopies = async () => {
    setBusy(true);
    try {
      const body = { copyCode: addForm.copyCode || null, quantity: addForm.copyCode ? 1 : Number(addForm.quantity), acquiredDate: addForm.acquiredDate || null, notes: addForm.notes || null };
      const res = await api.post(`/books/${id}/copies`, body);
      toast.success(`Added ${res.data.map((c) => c.copyCode).join(', ')}`);
      setAddOpen(false);
      setAddForm({ copyCode: '', quantity: 1, acquiredDate: todayIso(), notes: '' });
      reloadAll();
    } catch (e) { toast.error(errorMessage(e)); } finally { setBusy(false); }
  };

  const updateCopy = async () => {
    setBusy(true);
    try {
      const res = await api.put(`/books/copies/${statusCopy.id}`, { status: statusCopy.status, notes: statusCopy.notes || null });
      toast.success(res.data.status === 'RESERVED' ? `${res.data.copyCode} is now held for the next reservation` : `${res.data.copyCode} marked ${res.data.status.toLowerCase()}`);
      setStatusCopy(null);
      reloadAll();
    } catch (e) { toast.error(errorMessage(e)); } finally { setBusy(false); }
  };

  const remove = async () => {
    setBusy(true);
    try {
      await api.delete(`/books/${id}`);
      toast.success('Book removed from the catalogue. Borrowing history is kept.');
      navigate('/books');
    } catch (e) { toast.error(errorMessage(e)); setConfirmDelete(false); } finally { setBusy(false); }
  };

  if (book.loading && !book.data) return <Loading />;
  if (book.error) return <ErrorAlert message={book.error} />;
  const b = book.data;

  return (
    <>
      <nav aria-label="breadcrumb"><ol className="breadcrumb small"><li className="breadcrumb-item"><Link to="/books">Books</Link></li><li className="breadcrumb-item active">{b.title}</li></ol></nav>
      <PageHeader title={b.title} subtitle={b.subtitle}
        actions={<>
          <button className="btn btn-outline-primary" onClick={() => setEditing(true)}><i className="bi bi-pencil me-1" />Edit</button>
          {isAdmin && <button className="btn btn-outline-danger" onClick={() => setConfirmDelete(true)}><i className="bi bi-trash me-1" />Delete</button>}
        </>} />
      <div className="row g-3 mb-4">
        <div className="col-lg-8">
          <div className="panel d-flex flex-column flex-sm-row gap-4">
            <BookCover book={b} size="lg" />
            <dl className="detail-list flex-grow-1 mb-0">
              <dt>Author</dt><dd>{b.authorName}</dd>
              <dt>Publisher</dt><dd>{b.publisherName || '—'}</dd>
              <dt>Category</dt><dd>{b.categoryName}</dd>
              <dt>ISBN</dt><dd>{b.isbn}</dd>
              <dt>Edition / year</dt><dd>{b.edition || '—'} {b.publicationYear ? `(${b.publicationYear})` : ''}</dd>
              <dt>Language</dt><dd>{b.language}</dd>
              <dt>Shelf</dt><dd>{b.shelfNumber || '—'}</dd>
              <dt>Added</dt><dd>{formatDate(b.createdAt)}</dd>
              {b.description && <><dt>About</dt><dd>{b.description}</dd></>}
            </dl>
          </div>
        </div>
        <div className="col-lg-4">
          <div className="panel h-100">
            <h2 className="panel-title">Availability</h2>
            <div className="availability-number">{b.availableCopies}<span> of {b.totalCopies} copies on the shelf</span></div>
            {b.activeReservations > 0 && <p className="mb-2"><StatusBadge status="RESERVED" label={`${b.activeReservations} in the reservation queue`} /></p>}
            <Link to={`/issue?book=${b.id}`} className={`btn btn-primary w-100 mt-2 ${b.availableCopies === 0 ? 'disabled' : ''}`}>Issue a copy</Link>
          </div>
        </div>
      </div>

      <div className="panel">
        <div className="d-flex justify-content-between align-items-center mb-2">
          <h2 className="panel-title mb-0">Physical copies</h2>
          <button className="btn btn-sm btn-primary" onClick={() => setAddOpen(true)}><i className="bi bi-plus-lg me-1" />Add copies</button>
        </div>
        {copies.loading && !copies.data ? <Loading /> : copies.data?.length === 0 ? <EmptyState icon="upc" title="No copies registered yet" /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Copy code</th><th>Status</th><th>With</th><th>Due</th><th>Acquired</th><th>Notes</th><th /></tr></thead>
              <tbody>
                {copies.data?.map((c) => (
                  <tr key={c.id}>
                    <td><code className="fs-6">{c.copyCode}</code></td>
                    <td><StatusBadge status={c.status} /></td>
                    <td>{c.currentBorrower || '—'}</td>
                    <td>{c.dueDate ? <DueStamp date={c.dueDate} /> : '—'}</td>
                    <td>{formatDate(c.acquiredDate)}</td>
                    <td className="small">{c.notes || '—'}</td>
                    <td className="text-end">
                      {c.status !== 'BORROWED' && c.status !== 'RESERVED' && (
                        <button className="btn btn-sm btn-outline-secondary" onClick={() => setStatusCopy({ ...c })}>Change status</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <BookForm show={editing} book={b} onClose={() => setEditing(false)} onSaved={() => { setEditing(false); book.reload(); }} />

      <Modal show={addOpen} title="Add copies" onClose={() => setAddOpen(false)}
        footer={<><button className="btn btn-light" onClick={() => setAddOpen(false)}>Cancel</button>
          <button className="btn btn-primary" onClick={addCopies} disabled={busy}>Add copies</button></>}>
        <div className="row g-3">
          <Field label="Copy code (optional)" name="copyCode" className="col-12" hint="Leave empty to generate codes automatically">
            <input id="copyCode" className="form-control" value={addForm.copyCode} placeholder="e.g. LIB-CC-004"
              onChange={(e) => setAddForm({ ...addForm, copyCode: e.target.value.toUpperCase() })} />
          </Field>
          <Field label="How many" name="quantity">
            <input id="quantity" type="number" min={1} max={50} className="form-control" disabled={!!addForm.copyCode}
              value={addForm.copyCode ? 1 : addForm.quantity} onChange={(e) => setAddForm({ ...addForm, quantity: e.target.value })} />
          </Field>
          <Field label="Acquired on" name="acquiredDate">
            <input id="acquiredDate" type="date" className="form-control" value={addForm.acquiredDate} onChange={(e) => setAddForm({ ...addForm, acquiredDate: e.target.value })} />
          </Field>
          <Field label="Notes" name="notes" className="col-12">
            <input id="notes" className="form-control" value={addForm.notes} onChange={(e) => setAddForm({ ...addForm, notes: e.target.value })} />
          </Field>
        </div>
        {b.activeReservations > 0 && <div className="alert alert-warning small mt-3 mb-0">Members are waiting for this book. New copies will be held for them first.</div>}
      </Modal>

      <Modal show={!!statusCopy} title={`Change status of ${statusCopy?.copyCode}`} onClose={() => setStatusCopy(null)}
        footer={<><button className="btn btn-light" onClick={() => setStatusCopy(null)}>Cancel</button>
          <button className="btn btn-primary" onClick={updateCopy} disabled={busy}>Save status</button></>}>
        {statusCopy && (
          <div className="row g-3">
            <Field label="Status" name="copyStatus" className="col-12">
              <select id="copyStatus" className="form-select" value={statusCopy.status} onChange={(e) => setStatusCopy({ ...statusCopy, status: e.target.value })}>
                {MANUAL.map((s) => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
              </select>
            </Field>
            <Field label="Notes" name="copyNotes" className="col-12">
              <input id="copyNotes" className="form-control" value={statusCopy.notes || ''} onChange={(e) => setStatusCopy({ ...statusCopy, notes: e.target.value })} />
            </Field>
            <p className="small text-body-secondary mb-0">Borrowed and reserved statuses are set automatically by issue, return and reservations.</p>
          </div>
        )}
      </Modal>

      <ConfirmDialog show={confirmDelete} title="Delete this book?" busy={busy} confirmLabel="Delete book"
        message={`"${b.title}" will be removed from the catalogue and its copies withdrawn. Past borrowings and fines stay in the history.`}
        onConfirm={remove} onCancel={() => setConfirmDelete(false)} />
    </>
  );
}
