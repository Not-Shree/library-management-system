import { useState } from 'react';
import api, { errorMessage, fieldErrors } from '../../api/client.js';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { ConfirmDialog, EmptyState, ErrorAlert, Field, Loading, Modal, PageHeader, SearchBox } from '../../components/Common.jsx';

/** One page component for the three small lookup tables. */
const KINDS = {
  authors: { title: 'Authors', singular: 'author', fields: [{ name: 'name', label: 'Name' }, { name: 'biography', label: 'Biography', textarea: true }], extra: 'biography' },
  publishers: { title: 'Publishers', singular: 'publisher', fields: [{ name: 'name', label: 'Name' }, { name: 'address', label: 'Address' }, { name: 'website', label: 'Website' }], extra: 'address' },
  categories: { title: 'Categories', singular: 'category', fields: [{ name: 'name', label: 'Name' }, { name: 'description', label: 'Description' }], extra: 'description' },
};

export default function LookupPage({ kind }) {
  const cfg = KINDS[kind];
  const toast = useToast();
  const { isAdmin } = useAuth();
  const [q, setQ] = useState('');
  const search = useDebounced(q);
  const { data, loading, error, reload } = useApi(`/${kind}`, { q: search || undefined });
  const [editing, setEditing] = useState(null);   // {} for new, object for edit
  const [errors, setErrors] = useState({});
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);

  const save = async () => {
    setBusy(true); setErrors({});
    const body = Object.fromEntries(cfg.fields.map((f) => [f.name, editing[f.name] || null]));
    try {
      if (editing.id) await api.put(`/${kind}/${editing.id}`, body); else await api.post(`/${kind}`, body);
      toast.success(`${cfg.singular.charAt(0).toUpperCase() + cfg.singular.slice(1)} saved`);
      setEditing(null);
      reload();
    } catch (e) { setErrors(fieldErrors(e)); toast.error(errorMessage(e)); } finally { setBusy(false); }
  };

  const remove = async () => {
    setBusy(true);
    try { await api.delete(`/${kind}/${deleting.id}`); toast.success('Deleted'); reload(); } catch (e) { toast.error(errorMessage(e)); }
    finally { setBusy(false); setDeleting(null); }
  };

  return (
    <>
      <PageHeader title={cfg.title} actions={<button className="btn btn-primary" onClick={() => { setErrors({}); setEditing({}); }}><i className="bi bi-plus-lg me-1" />Add {cfg.singular}</button>} />
      <SearchBox value={q} onChange={setQ} placeholder={`Search ${kind}`} className="mb-3 search-narrow" />
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.length === 0 ? <EmptyState title={`No ${kind} yet`} /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Name</th><th>{cfg.fields.find((f) => f.name === cfg.extra).label}</th><th>Books</th><th /></tr></thead>
              <tbody>
                {data?.map((row) => (
                  <tr key={row.id}>
                    <td className="fw-semibold">{row.name}</td>
                    <td className="small text-body-secondary cell-clamp">{row[cfg.extra] || '—'}</td>
                    <td>{row.bookCount}</td>
                    <td className="text-end text-nowrap">
                      <button className="btn btn-sm btn-outline-secondary me-2" onClick={() => { setErrors({}); setEditing({ ...row }); }}>Edit</button>
                      {isAdmin && <button className="btn btn-sm btn-outline-danger" disabled={row.bookCount > 0}
                        title={row.bookCount > 0 ? 'Linked to books' : undefined} onClick={() => setDeleting(row)}>Delete</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal show={!!editing} title={editing?.id ? `Edit ${cfg.singular}` : `Add ${cfg.singular}`} onClose={() => setEditing(null)}
        footer={<><button className="btn btn-light" onClick={() => setEditing(null)}>Cancel</button>
          <button className="btn btn-primary" onClick={save} disabled={busy || !editing?.name}>Save</button></>}>
        {editing && (
          <div className="row g-3">
            {cfg.fields.map((f) => (
              <Field key={f.name} label={f.label} name={f.name} error={errors[f.name]} className="col-12">
                {f.textarea
                  ? <textarea id={f.name} rows={3} className="form-control" value={editing[f.name] || ''} onChange={(e) => setEditing({ ...editing, [f.name]: e.target.value })} />
                  : <input id={f.name} className={`form-control ${errors[f.name] ? 'is-invalid' : ''}`} value={editing[f.name] || ''} onChange={(e) => setEditing({ ...editing, [f.name]: e.target.value })} />}
              </Field>
            ))}
          </div>
        )}
      </Modal>
      <ConfirmDialog show={!!deleting} title={`Delete ${cfg.singular}?`} message={`"${deleting?.name}" will be deleted permanently.`}
        busy={busy} confirmLabel="Delete" onConfirm={remove} onCancel={() => setDeleting(null)} />
    </>
  );
}
