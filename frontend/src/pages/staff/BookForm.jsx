import { useEffect, useState } from 'react';
import api, { errorMessage, fieldErrors } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';
import { Field, Modal } from '../../components/Common.jsx';
import { blanksToNull } from '../../utils/format.js';

const EMPTY = { isbn: '', title: '', subtitle: '', authorId: '', publisherId: '', categoryId: '', language: 'English', edition: '',
  publicationYear: '', description: '', shelfNumber: '', coverImageUrl: '', initialCopies: 1 };

/** Add / edit book modal. On create it can register the first physical copies too. */
export default function BookForm({ show, book, onClose, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [lookups, setLookups] = useState({ authors: [], publishers: [], categories: [] });

  useEffect(() => {
    if (!show) return;
    setErrors({});
    setForm(book ? {
      ...EMPTY, ...Object.fromEntries(Object.entries(book).map(([k, v]) => [k, v ?? ''])), initialCopies: '',
    } : EMPTY);
    Promise.all([api.get('/authors'), api.get('/publishers'), api.get('/categories')])
      .then(([a, p, c]) => setLookups({ authors: a.data, publishers: p.data, categories: c.data }))
      .catch((e) => toast.error(errorMessage(e)));
  }, [show, book, toast]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const input = (name, props = {}) => (
    <input id={name} className={`form-control ${errors[name] ? 'is-invalid' : ''}`} value={form[name]} onChange={set(name)} {...props} />
  );
  const select = (name, options, placeholder) => (
    <select id={name} className={`form-select ${errors[name] ? 'is-invalid' : ''}`} value={form[name]} onChange={set(name)}>
      <option value="">{placeholder}</option>
      {options.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
    </select>
  );

  const save = async () => {
    setBusy(true); setErrors({});
    const body = blanksToNull({
      isbn: form.isbn, title: form.title, subtitle: form.subtitle, authorId: form.authorId, publisherId: form.publisherId,
      categoryId: form.categoryId, language: form.language, edition: form.edition, publicationYear: form.publicationYear,
      description: form.description, shelfNumber: form.shelfNumber, coverImageUrl: form.coverImageUrl,
      initialCopies: book ? '' : form.initialCopies,
    });
    ['authorId', 'publisherId', 'categoryId', 'publicationYear', 'initialCopies'].forEach((k) => { if (body[k] != null) body[k] = Number(body[k]); });
    try {
      const res = book ? await api.put(`/books/${book.id}`, body) : await api.post('/books', body);
      toast.success(book ? 'Book updated' : `"${res.data.title}" added with ${res.data.totalCopies} copies`);
      onSaved(res.data);
    } catch (e) {
      setErrors(fieldErrors(e));
      toast.error(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal show={show} title={book ? 'Edit book' : 'Add book'} onClose={onClose} size="lg"
      footer={<>
        <button className="btn btn-light" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={save} disabled={busy}>
          {busy && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}{book ? 'Save changes' : 'Add book'}
        </button>
      </>}>
      <div className="row g-3">
        <Field label="Title" name="title" error={errors.title} className="col-md-8">{input('title')}</Field>
        <Field label="ISBN" name="isbn" error={errors.isbn} className="col-md-4">{input('isbn', { placeholder: '978…' })}</Field>
        <Field label="Subtitle" name="subtitle" error={errors.subtitle} className="col-12">{input('subtitle')}</Field>
        <Field label="Author" name="authorId" error={errors.authorId} className="col-md-4" hint="Add new authors on the Authors page">{select('authorId', lookups.authors, 'Choose author')}</Field>
        <Field label="Publisher" name="publisherId" error={errors.publisherId} className="col-md-4">{select('publisherId', lookups.publishers, 'No publisher')}</Field>
        <Field label="Category" name="categoryId" error={errors.categoryId} className="col-md-4">{select('categoryId', lookups.categories, 'Choose category')}</Field>
        <Field label="Language" name="language" error={errors.language} className="col-md-3">{input('language')}</Field>
        <Field label="Edition" name="edition" error={errors.edition} className="col-md-3">{input('edition', { placeholder: 'e.g. 3rd' })}</Field>
        <Field label="Publication year" name="publicationYear" error={errors.publicationYear} className="col-md-3">{input('publicationYear', { type: 'number' })}</Field>
        <Field label="Shelf number" name="shelfNumber" error={errors.shelfNumber} className="col-md-3">{input('shelfNumber', { placeholder: 'e.g. CS-B2' })}</Field>
        <Field label="Cover image URL" name="coverImageUrl" error={errors.coverImageUrl} className="col-12"
          hint="Optional. Free covers: https://covers.openlibrary.org/b/isbn/<ISBN>-M.jpg">{input('coverImageUrl')}</Field>
        <Field label="Description" name="description" error={errors.description} className="col-12">
          <textarea id="description" rows={3} className="form-control" value={form.description} onChange={set('description')} />
        </Field>
        {!book && (
          <Field label="Physical copies to register now" name="initialCopies" error={errors.initialCopies} className="col-md-5"
            hint="Copy codes are generated automatically, e.g. LIB-0021-001">{input('initialCopies', { type: 'number', min: 0, max: 50 })}</Field>
        )}
      </div>
    </Modal>
  );
}
