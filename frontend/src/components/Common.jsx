import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { daysUntil, formatDate, titleCase } from '../utils/format.js';

/* ------------------------------------------------------------------ status badges */
// Colours follow the specification: available green, borrowed blue, overdue red,
// reserved yellow, paid green, pending yellow.
const BADGE = {
  AVAILABLE: 'success', BORROWED: 'primary', OVERDUE: 'danger', RESERVED: 'warning', RETURNED: 'secondary',
  PAID: 'success', PENDING: 'warning', PARTIALLY_PAID: 'info', PARTIAL: 'info', WAIVED: 'secondary',
  WAITING: 'warning', FULFILLED: 'success', CANCELLED: 'secondary', EXPIRED: 'dark',
  ACTIVE: 'success', INACTIVE: 'secondary', DAMAGED: 'danger', LOST: 'dark', WITHDRAWN: 'secondary',
  GOOD: 'success', ADMIN: 'dark', LIBRARIAN: 'primary', MEMBER: 'info',
};
const LABEL = { AVAILABLE: 'Available', PARTIALLY_PAID: 'Part paid', AVAILABLE_HOLD: 'Ready to collect' };

export function StatusBadge({ status, label }) {
  if (!status) return null;
  const variant = BADGE[status] || 'secondary';
  const text = label || LABEL[status] || titleCase(status);
  return <span className={`badge rounded-pill text-bg-${variant} status-badge`}>{text}</span>;
}

/** Reservation status "AVAILABLE" means "ready to collect", which reads better for members. */
export function ReservationBadge({ status }) {
  return status === 'AVAILABLE' ? <StatusBadge status="AVAILABLE" label="Ready to collect" /> : <StatusBadge status={status} />;
}

/* ------------------------------------------------------------------ due date "stamp" */
export function DueStamp({ date, returned }) {
  if (!date) return '—';
  const days = daysUntil(date);
  let tone = 'ok';
  let hint = days === 0 ? 'due today' : days > 0 ? `in ${days} day${days === 1 ? '' : 's'}` : `${-days} day${days === -1 ? '' : 's'} late`;
  if (returned) { tone = 'muted'; hint = null; }
  else if (days < 0) tone = 'late';
  else if (days <= 2) tone = 'soon';
  return (
    <span className={`due-stamp due-${tone}`}>
      <span className="due-date">{formatDate(date)}</span>
      {hint && <span className="due-hint">{hint}</span>}
    </span>
  );
}

/* ------------------------------------------------------------------ layout helpers */
export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="page-header d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
      <div>
        <h1 className="page-title">{title}</h1>
        {subtitle && <p className="text-body-secondary mb-0">{subtitle}</p>}
      </div>
      {actions && <div className="d-flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function Loading({ text = 'Loading…' }) {
  return (
    <div className="d-flex align-items-center gap-2 text-body-secondary py-5 justify-content-center" role="status">
      <span className="spinner-border spinner-border-sm" aria-hidden="true" />
      <span>{text}</span>
    </div>
  );
}

export function ErrorAlert({ message, onRetry }) {
  if (!message) return null;
  return (
    <div className="alert alert-danger d-flex justify-content-between align-items-center" role="alert">
      <span><i className="bi bi-exclamation-triangle me-2" aria-hidden="true" />{message}</span>
      {onRetry && <button className="btn btn-sm btn-outline-danger" onClick={onRetry}>Try again</button>}
    </div>
  );
}

export function EmptyState({ icon = 'inbox', title, children }) {
  return (
    <div className="empty-state text-center py-5">
      <i className={`bi bi-${icon} display-6 text-body-tertiary`} aria-hidden="true" />
      <h2 className="h6 mt-3 mb-1">{title}</h2>
      {children && <div className="text-body-secondary small">{children}</div>}
    </div>
  );
}

export function StatCard({ label, value, icon, tone = 'default', to, note }) {
  const body = (
    <div className={`stat-card stat-${tone} h-100`}>
      <div className="d-flex justify-content-between align-items-start">
        <span className="stat-label">{label}</span>
        {icon && <i className={`bi bi-${icon} stat-icon`} aria-hidden="true" />}
      </div>
      <div className="stat-value">{value ?? '—'}</div>
      {note && <div className="stat-note">{note}</div>}
    </div>
  );
  return to ? <Link to={to} className="text-decoration-none text-reset d-block h-100">{body}</Link> : body;
}

/* ------------------------------------------------------------------ pagination */
export function Pagination({ page, totalPages, totalElements, onChange }) {
  if (!totalPages || totalPages <= 1) {
    return totalElements ? <div className="small text-body-secondary mt-2">{totalElements} record{totalElements === 1 ? '' : 's'}</div> : null;
  }
  const pages = [];
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  for (let i = start; i < Math.min(totalPages, start + 5); i++) pages.push(i);
  return (
    <nav className="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3" aria-label="Pagination">
      <span className="small text-body-secondary">Page {page + 1} of {totalPages} · {totalElements} records</span>
      <ul className="pagination pagination-sm mb-0">
        <li className={`page-item ${page === 0 ? 'disabled' : ''}`}>
          <button className="page-link" onClick={() => onChange(page - 1)} aria-label="Previous page"><i className="bi bi-chevron-left" /></button>
        </li>
        {pages.map((p) => (
          <li key={p} className={`page-item ${p === page ? 'active' : ''}`}>
            <button className="page-link" onClick={() => onChange(p)} aria-current={p === page ? 'page' : undefined}>{p + 1}</button>
          </li>
        ))}
        <li className={`page-item ${page >= totalPages - 1 ? 'disabled' : ''}`}>
          <button className="page-link" onClick={() => onChange(page + 1)} aria-label="Next page"><i className="bi bi-chevron-right" /></button>
        </li>
      </ul>
    </nav>
  );
}

/* ------------------------------------------------------------------ modal + confirm dialog */
export function Modal({ show, title, onClose, children, footer, size }) {
  const dialogRef = useRef(null);
  useEffect(() => {
    if (!show) return undefined;
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', onKey);
    document.body.classList.add('modal-open');
    dialogRef.current?.querySelector('input, select, textarea, button')?.focus();
    return () => { document.removeEventListener('keydown', onKey); document.body.classList.remove('modal-open'); };
  }, [show, onClose]);
  if (!show) return null;
  return (
    <>
      <div className="modal-backdrop fade show" />
      <div className="modal fade show d-block" tabIndex="-1" role="dialog" aria-modal="true" aria-label={title}
        onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
        <div className={`modal-dialog modal-dialog-centered modal-dialog-scrollable ${size ? `modal-${size}` : ''}`} ref={dialogRef}>
          <div className="modal-content">
            <div className="modal-header">
              <h2 className="modal-title h5">{title}</h2>
              <button type="button" className="btn-close" aria-label="Close" onClick={onClose} />
            </div>
            <div className="modal-body">{children}</div>
            {footer && <div className="modal-footer">{footer}</div>}
          </div>
        </div>
      </div>
    </>
  );
}

export function ConfirmDialog({ show, title, message, confirmLabel = 'Confirm', variant = 'danger', busy, onConfirm, onCancel }) {
  return (
    <Modal show={show} title={title} onClose={onCancel}
      footer={<>
        <button className="btn btn-light" onClick={onCancel} disabled={busy}>Cancel</button>
        <button className={`btn btn-${variant}`} onClick={onConfirm} disabled={busy}>
          {busy && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}{confirmLabel}
        </button>
      </>}>
      <p className="mb-0">{message}</p>
    </Modal>
  );
}

/* ------------------------------------------------------------------ book cover */
const COVER_COLORS = ['#2E6B57', '#3B5A8A', '#8A3B3B', '#6B4E2E', '#4A3B6B', '#2E5F6B', '#7A5A1E', '#3F4F5F'];
export function BookCover({ book, size = 'md' }) {
  const initials = (book?.title || '?').split(/\s+/).slice(0, 2).map((w) => w[0]).join('').toUpperCase();
  const src = book?.coverImageUrl;
  // Fallback "cloth binding" colour picked from the title, so covers without an image still differ.
  const hash = [...(book?.title || '')].reduce((h, c) => (h * 31 + c.charCodeAt(0)) >>> 0, 7);
  const cover = COVER_COLORS[hash % COVER_COLORS.length];
  return (
    <div className={`book-cover cover-${size}`} style={{ '--cover': cover }} aria-hidden="true">
      <span className="cover-fallback">{initials}</span>
      {src && <img src={src} alt="" loading="lazy" onError={(e) => { e.currentTarget.style.display = 'none'; }}
        onLoad={(e) => { if (e.currentTarget.naturalWidth < 5) e.currentTarget.style.display = 'none'; }} />}
    </div>
  );
}

/* ------------------------------------------------------------------ form helpers */
export function Field({ label, name, error, hint, children, className = 'col-md-6' }) {
  return (
    <div className={className}>
      <label className="form-label" htmlFor={name}>{label}</label>
      {children}
      {error && <div className="invalid-feedback d-block">{error}</div>}
      {hint && !error && <div className="form-text">{hint}</div>}
    </div>
  );
}

export function SearchBox({ value, onChange, placeholder = 'Search…', className = '' }) {
  return (
    <div className={`input-group search-box ${className}`}>
      <span className="input-group-text"><i className="bi bi-search" aria-hidden="true" /></span>
      <input type="search" className="form-control" value={value} placeholder={placeholder} aria-label={placeholder}
        onChange={(e) => onChange(e.target.value)} />
    </div>
  );
}
