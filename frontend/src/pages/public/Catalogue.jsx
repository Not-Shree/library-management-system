import { useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { BookCover, EmptyState, ErrorAlert, Loading, Modal, PageHeader, Pagination, SearchBox, StatusBadge } from '../../components/Common.jsx';

/**
 * Book catalogue. Used on the public page (publicView) and as "Browse books" for members,
 * who can also reserve titles that have no free copy.
 */
export default function Catalogue({ publicView = false }) {
  const { user, isMember } = useAuth();
  const toast = useToast();
  const [q, setQ] = useState('');
  const [filters, setFilters] = useState({ categoryId: '', authorId: '', language: '', available: '', yearFrom: '', yearTo: '' });
  const [sort, setSort] = useState('title');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [busy, setBusy] = useState(false);
  const search = useDebounced(q);

  const params = { q: search || undefined, page, size: 12, sort, dir: sort === 'year' ? 'desc' : 'asc' };
  Object.entries(filters).forEach(([k, v]) => { if (v !== '') params[k] = v; });
  const { data, loading, error, reload } = useApi('/books', params);
  const categories = useApi('/categories').data || [];
  const authors = useApi('/authors').data || [];
  const languages = useApi('/books/meta/languages').data || [];

  const setFilter = (k) => (e) => { setFilters({ ...filters, [k]: e.target.value }); setPage(0); };
  const clearFilters = () => { setFilters({ categoryId: '', authorId: '', language: '', available: '', yearFrom: '', yearTo: '' }); setQ(''); setPage(0); };

  const reserve = async (book) => {
    setBusy(true);
    try {
      const res = await api.post('/reservations', { bookId: book.id });
      toast.success(`Reserved "${book.title}". You are number ${res.data.queuePosition} in the queue.`);
      setSelected(null);
      reload();
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const body = (
    <>
      <PageHeader title={publicView ? 'Library catalogue' : 'Browse books'}
        subtitle="Search by title, ISBN, author, publisher or category." />
      <div className="filter-bar mb-4">
        <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Search title, ISBN, author…" className="filter-search" />
        <select className="form-select" value={filters.categoryId} onChange={setFilter('categoryId')} aria-label="Category">
          <option value="">All categories</option>{categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select className="form-select" value={filters.authorId} onChange={setFilter('authorId')} aria-label="Author">
          <option value="">All authors</option>{authors.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
        </select>
        <select className="form-select" value={filters.language} onChange={setFilter('language')} aria-label="Language">
          <option value="">Any language</option>{languages.map((l) => <option key={l} value={l}>{l}</option>)}
        </select>
        <select className="form-select" value={filters.available} onChange={setFilter('available')} aria-label="Availability">
          <option value="">Any availability</option><option value="true">On the shelf</option><option value="false">All copies out</option>
        </select>
        <input type="number" className="form-control" placeholder="Year from" value={filters.yearFrom} onChange={setFilter('yearFrom')} aria-label="Published from year" />
        <input type="number" className="form-control" placeholder="Year to" value={filters.yearTo} onChange={setFilter('yearTo')} aria-label="Published to year" />
        <select className="form-select" value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sort by">
          <option value="title">Sort: title</option><option value="author">Sort: author</option>
          <option value="year">Sort: newest edition</option><option value="newest">Sort: recently added</option>
        </select>
      </div>

      <ErrorAlert message={error} onRetry={reload} />
      {loading && !data ? <Loading /> : data?.content.length === 0 ? (
        <EmptyState icon="search" title="No books match these filters">
          <button className="btn btn-link" onClick={clearFilters}>Clear all filters</button>
        </EmptyState>
      ) : (
        <>
          <div className="book-grid">
            {data?.content.map((b) => (
              <button key={b.id} className="book-card" onClick={() => setSelected(b)}>
                <BookCover book={b} />
                <div className="book-card-body">
                  <div className="book-title">{b.title}</div>
                  <div className="small text-body-secondary">{b.authorName}{b.publicationYear ? `, ${b.publicationYear}` : ''}</div>
                  <div className="mt-2">
                    {b.availableCopies > 0
                      ? <StatusBadge status="AVAILABLE" label={`${b.availableCopies} of ${b.totalCopies} available`} />
                      : <StatusBadge status="BORROWED" label="Currently unavailable" />}
                  </div>
                </div>
              </button>
            ))}
          </div>
          <Pagination page={data?.page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
        </>
      )}

      <Modal show={!!selected} title={selected?.title} onClose={() => setSelected(null)} size="lg"
        footer={selected && (
          <>
            <button className="btn btn-light" onClick={() => setSelected(null)}>Close</button>
            {selected.availableCopies === 0 && selected.totalCopies > 0 && isMember && (
              <button className="btn btn-warning" disabled={busy} onClick={() => reserve(selected)}>
                <i className="bi bi-bookmark-plus me-1" />Currently unavailable — Reserve book
              </button>
            )}
            {selected.availableCopies === 0 && !user && (
              <Link to="/login" className="btn btn-warning">Log in to reserve</Link>
            )}
          </>
        )}>
        {selected && (
          <div className="d-flex flex-column flex-sm-row gap-4">
            <BookCover book={selected} size="lg" />
            <div className="flex-grow-1">
              {selected.subtitle && <p className="book-subtitle">{selected.subtitle}</p>}
              <dl className="detail-list">
                <dt>Author</dt><dd>{selected.authorName}</dd>
                <dt>Publisher</dt><dd>{selected.publisherName || '—'}</dd>
                <dt>Category</dt><dd>{selected.categoryName}</dd>
                <dt>ISBN</dt><dd>{selected.isbn}</dd>
                <dt>Edition</dt><dd>{selected.edition || '—'}{selected.publicationYear ? ` (${selected.publicationYear})` : ''}</dd>
                <dt>Language</dt><dd>{selected.language}</dd>
                <dt>Shelf</dt><dd>{selected.shelfNumber || '—'}</dd>
                <dt>Availability</dt>
                <dd>
                  {selected.availableCopies > 0
                    ? <StatusBadge status="AVAILABLE" label={`${selected.availableCopies} of ${selected.totalCopies} on the shelf`} />
                    : <StatusBadge status="BORROWED" label="All copies are out" />}
                  {selected.activeReservations > 0 && <span className="small text-body-secondary ms-2">{selected.activeReservations} in the reservation queue</span>}
                </dd>
              </dl>
              {selected.description && <p className="mb-0">{selected.description}</p>}
              {selected.availableCopies > 0 && isMember && (
                <p className="small text-body-secondary mt-3 mb-0">Collect a copy from shelf {selected.shelfNumber || 'at the desk'} and have it issued at the counter.</p>
              )}
            </div>
          </div>
        )}
      </Modal>
    </>
  );

  if (!publicView) return body;
  return (
    <div className="public-page">
      <header className="public-header">
        <Link to="/" className="brand-dark"><i className="bi bi-book-half" aria-hidden="true" /> College Library</Link>
        <div className="d-flex gap-2">
          {user ? <Link to="/dashboard" className="btn btn-primary btn-sm">Go to my dashboard</Link> : (
            <><Link to="/login" className="btn btn-outline-primary btn-sm">Log in</Link><Link to="/register" className="btn btn-primary btn-sm">Register</Link></>
          )}
        </div>
      </header>
      <main className="container-xl py-4">{body}</main>
    </div>
  );
}
