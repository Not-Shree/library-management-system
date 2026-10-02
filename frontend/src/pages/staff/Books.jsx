import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { BookCover, EmptyState, ErrorAlert, Loading, PageHeader, Pagination, SearchBox, StatusBadge } from '../../components/Common.jsx';
import BookForm from './BookForm.jsx';

export default function Books() {
  const navigate = useNavigate();
  const { isStaff } = useAuth();
  const [q, setQ] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [available, setAvailable] = useState('');
  const [sort, setSort] = useState('title');
  const [page, setPage] = useState(0);
  const [showForm, setShowForm] = useState(false);
  const search = useDebounced(q);
  const params = { q: search || undefined, categoryId: categoryId || undefined, available: available || undefined, page, size: 10, sort, dir: sort === 'newest' || sort === 'year' ? 'desc' : 'asc' };
  const { data, loading, error, reload } = useApi('/books', params);
  const categories = useApi('/categories').data || [];

  return (
    <>
      <PageHeader title="Books" subtitle="Titles in the catalogue. Open a book to manage its physical copies."
        actions={isStaff && <button className="btn btn-primary" onClick={() => setShowForm(true)}><i className="bi bi-plus-lg me-1" />Add book</button>} />
      <div className="filter-bar mb-3">
        <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Title, ISBN, author, publisher…" className="filter-search" />
        <select className="form-select" value={categoryId} onChange={(e) => { setCategoryId(e.target.value); setPage(0); }} aria-label="Category">
          <option value="">All categories</option>{categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select className="form-select" value={available} onChange={(e) => { setAvailable(e.target.value); setPage(0); }} aria-label="Availability">
          <option value="">Any availability</option><option value="true">On the shelf</option><option value="false">All copies out</option>
        </select>
        <select className="form-select" value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sort by">
          <option value="title">Sort: title</option><option value="author">Sort: author</option>
          <option value="year">Sort: publication year</option><option value="newest">Sort: recently added</option>
        </select>
      </div>
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.content.length === 0 ? <EmptyState icon="book" title="No books found">Try a different search, or add a book.</EmptyState> : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead><tr><th>Book</th><th>Author</th><th>Category</th><th>Shelf</th><th>Copies</th><th>Status</th></tr></thead>
              <tbody>
                {data?.content.map((b) => (
                  <tr key={b.id} className="row-link" onClick={() => navigate(`/books/${b.id}`)}>
                    <td>
                      <div className="d-flex align-items-center gap-3">
                        <BookCover book={b} size="sm" />
                        <div>
                          <Link to={`/books/${b.id}`} className="fw-semibold" onClick={(e) => e.stopPropagation()}>{b.title}</Link>
                          <div className="small text-body-secondary">ISBN {b.isbn}{b.edition ? ` · ${b.edition} ed.` : ''}</div>
                        </div>
                      </div>
                    </td>
                    <td>{b.authorName}</td>
                    <td>{b.categoryName}</td>
                    <td>{b.shelfNumber || '—'}</td>
                    <td className="text-nowrap">{b.availableCopies} / {b.totalCopies}</td>
                    <td>
                      {b.totalCopies === 0 ? <StatusBadge status="WITHDRAWN" label="No copies" />
                        : b.availableCopies > 0 ? <StatusBadge status="AVAILABLE" /> : <StatusBadge status="BORROWED" label="All out" />}
                      {b.activeReservations > 0 && <StatusBadge status="RESERVED" label={`${b.activeReservations} reserved`} />}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      <Pagination page={data?.page} totalPages={data?.totalPages} totalElements={data?.totalElements} onChange={setPage} />
      <BookForm show={showForm} onClose={() => setShowForm(false)} onSaved={(b) => { setShowForm(false); navigate(`/books/${b.id}`); }} />
    </>
  );
}
