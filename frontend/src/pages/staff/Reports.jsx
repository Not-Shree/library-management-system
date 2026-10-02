import { useState } from 'react';
import api, { downloadFile, errorMessage } from '../../api/client.js';
import { useApi } from '../../hooks/useApi.js';
import { useToast } from '../../context/ToastContext.jsx';
import { EmptyState, ErrorAlert, Loading, PageHeader } from '../../components/Common.jsx';
import { addDays, formatDate, money, todayIso } from '../../utils/format.js';

// Reports whose results depend on the chosen date range.
const DATED = ['returned', 'fine-collection', 'most-borrowed', 'active-members', 'reservations'];
const isDate = (v) => typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v);

function renderCell(value, column) {
  if (value === null || value === undefined || value === '') return '—';
  if (column.includes('₹')) return money(value);
  if (isDate(value)) return formatDate(value);
  return String(value);
}

export default function Reports() {
  const toast = useToast();
  const types = useApi('/reports');
  const [type, setType] = useState('overdue');
  const [from, setFrom] = useState(addDays(todayIso(), -30));
  const [to, setTo] = useState(todayIso());
  const [table, setTable] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const params = () => (DATED.includes(type) ? { from, to } : {});

  const run = async () => {
    setLoading(true); setError('');
    try { setTable((await api.get(`/reports/${type}`, { params: params() })).data); }
    catch (e) { setError(errorMessage(e)); setTable(null); } finally { setLoading(false); }
  };

  const csv = async () => {
    try { await downloadFile(`/reports/${type}`, { ...params(), format: 'csv' }, `${type}-report.csv`); }
    catch (e) { toast.error(errorMessage(e)); }
  };

  return (
    <>
      <PageHeader title="Reports" subtitle="Run a report on screen or download it as CSV (opens in Excel)." />
      <div className="panel mb-3">
        <div className="row g-3 align-items-end">
          <div className="col-md-4">
            <label className="form-label" htmlFor="reportType">Report</label>
            <select id="reportType" className="form-select" value={type} onChange={(e) => { setType(e.target.value); setTable(null); }}>
              {Object.entries(types.data || {}).map(([k, label]) => <option key={k} value={k}>{label}</option>)}
            </select>
          </div>
          <div className="col-6 col-md-2">
            <label className="form-label" htmlFor="from">From</label>
            <input id="from" type="date" className="form-control" value={from} max={to} disabled={!DATED.includes(type)} onChange={(e) => setFrom(e.target.value)} />
          </div>
          <div className="col-6 col-md-2">
            <label className="form-label" htmlFor="to">To</label>
            <input id="to" type="date" className="form-control" value={to} min={from} disabled={!DATED.includes(type)} onChange={(e) => setTo(e.target.value)} />
          </div>
          <div className="col-md-4 d-flex gap-2">
            <button className="btn btn-primary flex-grow-1" onClick={run} disabled={loading}>{loading && <span className="spinner-border spinner-border-sm me-2" />}Run report</button>
            <button className="btn btn-outline-primary" onClick={csv}><i className="bi bi-filetype-csv me-1" />CSV</button>
            <button className="btn btn-outline-secondary" disabled title="PDF export is not implemented in this version"><i className="bi bi-filetype-pdf" /></button>
          </div>
        </div>
        {!DATED.includes(type) && <div className="form-text mt-2">This report shows the current state, so the date range does not apply.</div>}
      </div>
      <ErrorAlert message={error} />
      {loading ? <Loading text="Running report…" /> : table && (
        <div className="panel">
          <div className="d-flex justify-content-between align-items-baseline flex-wrap gap-2">
            <h2 className="panel-title mb-0">{table.title}</h2>
            <span className="small text-body-secondary">{table.description} · {table.rows.length} rows</span>
          </div>
          {table.rows.length === 0 ? <EmptyState icon="clipboard-data" title="No rows for this report" /> : (
            <div className="table-responsive mt-3">
              <table className="table table-sm table-striped align-middle mb-0">
                <thead><tr>{table.columns.map((c) => <th key={c} className="text-nowrap">{c}</th>)}</tr></thead>
                <tbody>{table.rows.map((row, i) => (
                  <tr key={i}>{row.map((v, j) => <td key={j} className={table.columns[j].includes('₹') ? 'text-end' : ''}>{renderCell(v, table.columns[j])}</td>)}</tr>
                ))}</tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </>
  );
}
