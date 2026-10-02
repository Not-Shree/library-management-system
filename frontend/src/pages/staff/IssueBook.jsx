import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { useToast } from '../../context/ToastContext.jsx';
import { BookCover, PageHeader, StatusBadge } from '../../components/Common.jsx';
import MemberPicker from '../../components/MemberPicker.jsx';
import { addDays, formatDate, todayIso } from '../../utils/format.js';

/**
 * Issue desk: 1) pick the member  2) pick the book and a copy (or type/scan a copy code)  3) confirm.
 * All rules (limit, fines, availability) are checked again by the server.
 */
export default function IssueBook() {
  const toast = useToast();
  const [params] = useSearchParams();
  const [member, setMember] = useState(null);
  const [book, setBook] = useState(null);
  const [copyId, setCopyId] = useState(null);
  const [copyCode, setCopyCode] = useState('');
  const [bookQ, setBookQ] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [issued, setIssued] = useState(null);
  const search = useDebounced(bookQ, 300);
  const settings = useApi('/settings').data;
  const books = useApi('/books', { q: search, size: 6 }, { enabled: !book && search.trim().length >= 2 });
  const copies = useApi(book ? `/books/${book.id}/copies` : null, {}, { enabled: !!book });

  // Pre-select from links like /issue?member=3 or /issue?book=5
  useEffect(() => {
    const m = params.get('member');
    const b = params.get('book');
    if (m) api.get(`/members/${m}`).then((r) => setMember(r.data)).catch(() => {});
    if (b) api.get(`/books/${b}`).then((r) => setBook(r.data)).catch(() => {});
  }, [params]);

  const shelfCopies = (copies.data || []).filter((c) => c.status === 'AVAILABLE' || c.status === 'RESERVED');
  const canIssue = member && (copyId || copyCode.trim());
  const dueDate = settings ? addDays(todayIso(), settings.loanPeriodDays) : null;

  const issue = async () => {
    setBusy(true); setError('');
    try {
      const res = await api.post('/borrowings/issue', { memberId: member.id, bookCopyId: copyId, copyCode: copyId ? null : copyCode.trim() });
      setIssued(res.data);
      toast.success(`Issued "${res.data.bookTitle}" to ${res.data.memberName}`);
    } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  };

  const reset = () => { setIssued(null); setBook(null); setCopyId(null); setCopyCode(''); setBookQ(''); setError('');
    api.get(`/members/${member.id}`).then((r) => setMember(r.data)).catch(() => {}); };

  if (issued) {
    return (
      <>
        <PageHeader title="Book issued" />
        <div className="panel issue-done">
          <i className="bi bi-check-circle-fill text-success display-6" aria-hidden="true" />
          <h2 className="h4 mt-2">{issued.bookTitle}</h2>
          <p className="mb-1">Copy <code>{issued.copyCode}</code> issued to <strong>{issued.memberName}</strong> ({issued.memberCode})</p>
          <div className="due-banner my-3"><span>Due back on</span><strong>{formatDate(issued.dueDate)}</strong></div>
          <div className="d-flex gap-2 justify-content-center flex-wrap">
            <button className="btn btn-primary" onClick={reset}>Issue another book to {issued.memberName.split(' ')[0]}</button>
            <button className="btn btn-outline-primary" onClick={() => { setMember(null); reset(); }}>New member</button>
            <Link to="/borrowings" className="btn btn-light">View borrowings</Link>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Issue book" subtitle={settings ? `Loan period ${settings.loanPeriodDays} days · limit ${settings.maxBooksPerMember} books per member (unless set per member)` : undefined} />
      <div className="row g-3">
        <div className="col-lg-6">
          <div className="panel h-100">
            <h2 className="panel-title"><span className="step-no">1</span> Member</h2>
            <MemberPicker value={member} onChange={setMember} />
          </div>
        </div>
        <div className="col-lg-6">
          <div className="panel h-100">
            <h2 className="panel-title"><span className="step-no">2</span> Book copy</h2>
            {!book ? (
              <>
                <div className="position-relative">
                  <input className="form-control form-control-lg" value={bookQ} onChange={(e) => setBookQ(e.target.value)} placeholder="Search title, ISBN or author" aria-label="Find book" />
                  {search.trim().length >= 2 && (
                    <div className="list-group picker-results shadow-sm">
                      {books.loading && <div className="list-group-item small">Searching…</div>}
                      {books.data?.content.length === 0 && <div className="list-group-item small text-body-secondary">No book found</div>}
                      {books.data?.content.map((b) => (
                        <button key={b.id} type="button" className="list-group-item list-group-item-action d-flex gap-3 align-items-center" onClick={() => { setBook(b); setCopyCode(''); }}>
                          <BookCover book={b} size="sm" />
                          <div className="flex-grow-1 text-start">
                            <div className="fw-semibold">{b.title}</div>
                            <div className="small text-body-secondary">{b.authorName}</div>
                          </div>
                          {b.availableCopies > 0 ? <StatusBadge status="AVAILABLE" label={`${b.availableCopies} free`} /> : <StatusBadge status="BORROWED" label="All out" />}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
                <div className="text-center small text-body-secondary my-3">or type / scan a copy code</div>
                <input className="form-control" value={copyCode} onChange={(e) => setCopyCode(e.target.value.toUpperCase())} placeholder="e.g. LIB-CC-002" aria-label="Copy code" />
              </>
            ) : (
              <>
                <div className="d-flex gap-3 align-items-start mb-3">
                  <BookCover book={book} size="sm" />
                  <div className="flex-grow-1"><div className="fw-semibold">{book.title}</div><div className="small text-body-secondary">{book.authorName} · shelf {book.shelfNumber || '—'}</div></div>
                  <button className="btn btn-sm btn-link" onClick={() => { setBook(null); setCopyId(null); }}>Change</button>
                </div>
                {copies.loading ? <div className="small">Loading copies…</div> : shelfCopies.length === 0 ? (
                  <div className="alert alert-warning mb-0">No copy of this book is on the shelf.{' '}
                    {member && <Link to={`/reservations?book=${book.id}&member=${member.id}`}>Reserve it for {member.fullName}</Link>}</div>
                ) : (
                  <div className="d-flex flex-column gap-2" role="radiogroup" aria-label="Choose copy">
                    {shelfCopies.map((c) => (
                      <label key={c.id} className={`copy-option ${copyId === c.id ? 'selected' : ''}`}>
                        <input type="radio" name="copy" className="form-check-input me-2" checked={copyId === c.id} onChange={() => setCopyId(c.id)} />
                        <code>{c.copyCode}</code>
                        <span className="ms-auto">{c.status === 'RESERVED' ? <StatusBadge status="RESERVED" label="On hold for a reservation" /> : <StatusBadge status="AVAILABLE" />}</span>
                      </label>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
        </div>
        <div className="col-12">
          <div className="panel d-flex flex-wrap align-items-center gap-3">
            <h2 className="panel-title mb-0"><span className="step-no">3</span> Confirm</h2>
            {dueDate && <span className="text-body-secondary">Due date will be <strong className="text-body">{formatDate(dueDate)}</strong></span>}
            <button className="btn btn-primary btn-lg ms-auto" disabled={!canIssue || busy} onClick={issue}>
              {busy && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}Issue book
            </button>
            {error && <div className="alert alert-danger w-100 mb-0" role="alert"><i className="bi bi-x-octagon me-2" aria-hidden="true" />{error}</div>}
          </div>
        </div>
      </div>
    </>
  );
}
