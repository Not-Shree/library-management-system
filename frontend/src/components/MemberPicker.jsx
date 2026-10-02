import { useState } from 'react';
import { useApi, useDebounced } from '../hooks/useApi.js';
import { money } from '../utils/format.js';
import { StatusBadge } from './Common.jsx';

/** Search box that finds members by name, ID, email or phone and lets the librarian pick one. */
export default function MemberPicker({ value, onChange }) {
  const [q, setQ] = useState('');
  const search = useDebounced(q, 300);
  const { data, loading } = useApi('/members', { q: search, size: 6 }, { enabled: search.trim().length >= 2 && !value });

  if (value) {
    const overLimit = value.currentBorrowedBooks >= value.maxBooksAllowed;
    return (
      <div className="picked-card">
        <div className="d-flex justify-content-between align-items-start gap-2">
          <div>
            <div className="fw-semibold">{value.fullName} <StatusBadge status={value.status} /></div>
            <div className="small text-body-secondary">{value.memberCode} · {value.department || 'No department'} · {value.email}</div>
          </div>
          <button type="button" className="btn btn-sm btn-link" onClick={() => { onChange(null); setQ(''); }}>Change</button>
        </div>
        <div className="d-flex flex-wrap gap-3 mt-2 small">
          <span className={overLimit ? 'text-danger fw-semibold' : ''}>
            <i className="bi bi-journal-bookmark me-1" aria-hidden="true" />{value.currentBorrowedBooks} of {value.maxBooksAllowed} books out
          </span>
          <span className={Number(value.outstandingFine) > 0 ? 'text-danger fw-semibold' : ''}>
            <i className="bi bi-cash-coin me-1" aria-hidden="true" />Fines owed: {money(value.outstandingFine)}
          </span>
        </div>
      </div>
    );
  }

  return (
    <div className="position-relative">
      <input className="form-control form-control-lg" value={q} onChange={(e) => setQ(e.target.value)}
        placeholder="Type a name, member ID, email or phone" aria-label="Find member" autoComplete="off" />
      {search.trim().length >= 2 && (
        <div className="list-group picker-results shadow-sm">
          {loading && <div className="list-group-item small text-body-secondary">Searching…</div>}
          {!loading && data?.content.length === 0 && <div className="list-group-item small text-body-secondary">No member found</div>}
          {data?.content.map((m) => (
            <button type="button" key={m.id} className="list-group-item list-group-item-action" onClick={() => onChange(m)}>
              <div className="d-flex justify-content-between">
                <span className="fw-semibold">{m.fullName}</span>
                <StatusBadge status={m.status} />
              </div>
              <div className="small text-body-secondary">{m.memberCode} · {m.currentBorrowedBooks}/{m.maxBooksAllowed} books · fines {money(m.outstandingFine)}</div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
