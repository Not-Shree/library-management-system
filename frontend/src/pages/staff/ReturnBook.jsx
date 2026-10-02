import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { useApi, useDebounced } from '../../hooks/useApi.js';
import { useToast } from '../../context/ToastContext.jsx';
import { DueStamp, EmptyState, PageHeader, StatusBadge } from '../../components/Common.jsx';
import PaymentModal from '../../components/PaymentModal.jsx';
import { formatDate, money, todayIso } from '../../utils/format.js';

/**
 * mode="return": find the loan, preview the fine (nothing saved yet), then confirm the return.
 * mode="renew":  find the loan and extend the due date, if the renewal rules allow it.
 */
export default function ReturnBook({ mode = 'return' }) {
  const isReturn = mode === 'return';
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const [q, setQ] = useState('');
  const [selected, setSelected] = useState(null);
  const [returnDate, setReturnDate] = useState(todayIso());
  const [condition, setCondition] = useState('GOOD');
  const [remarks, setRemarks] = useState('');
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  const [paying, setPaying] = useState(null);
  const search = useDebounced(q, 300);
  const settings = useApi('/settings').data;
  const loans = useApi('/borrowings', { q: search || undefined, status: 'BORROWED', size: 8, sort: 'due', dir: 'asc' }, { enabled: !selected && !result });

  // Opened from a link such as /return?borrowing=12
  useEffect(() => {
    const id = params.get('borrowing');
    if (id) api.get(`/borrowings/${id}`).then((r) => { if (r.data.status === 'BORROWED') setSelected(r.data); }).catch(() => {});
  }, [params]);

  // Fine preview whenever the loan or the return date changes (return mode only).
  useEffect(() => {
    if (!isReturn || !selected || !returnDate) { setPreview(null); return; }
    setError('');
    api.get(`/borrowings/${selected.id}/return-preview`, { params: { returnDate } })
      .then((r) => setPreview(r.data))
      .catch((e) => { setPreview(null); setError(errorMessage(e)); });
  }, [isReturn, selected, returnDate]);

  const clear = () => {
    setSelected(null); setResult(null); setPreview(null); setError(''); setQ('');
    setReturnDate(todayIso()); setCondition('GOOD'); setRemarks(''); setParams({});
  };

  const confirmReturn = async () => {
    setBusy(true); setError('');
    try {
      const res = await api.post(`/borrowings/${selected.id}/return`, { returnDate, bookCondition: condition, remarks: remarks || null });
      setResult(res.data);
      toast.success(res.data.fine ? `Returned. Fine of ${money(res.data.fine.amount)} added.` : 'Returned on time. No fine.');
    } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  };

  const renew = async () => {
    setBusy(true); setError('');
    try {
      const res = await api.post(`/borrowings/${selected.id}/renew`);
      setResult({ borrowing: res.data, renewed: true });
      toast.success(`Renewed until ${formatDate(res.data.dueDate)}`);
    } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  };

  const title = isReturn ? 'Return book' : 'Renew book';

  /* ------------------------------------------------ finished */
  if (result) {
    const b = result.borrowing;
    return (
      <>
        <PageHeader title={title} />
        <div className="row g-3">
          <div className="col-lg-6">
            <div className="panel issue-done">
              <i className="bi bi-check-circle-fill text-success display-6" aria-hidden="true" />
              <h2 className="h5 mt-2">{b.bookTitle}</h2>
              <p className="mb-1"><code>{b.copyCode}</code> · {b.memberName} ({b.memberCode})</p>
              {result.renewed ? (
                <div className="due-banner my-3"><span>New due date</span><strong>{formatDate(b.dueDate)}</strong></div>
              ) : (
                <p className="text-body-secondary">Returned on {formatDate(b.returnDate)}{b.overdueDays > 0 ? `, ${b.overdueDays} days late` : ', on time'}.</p>
              )}
              {result.reservationNotice && (
                <div className="alert alert-warning text-start"><i className="bi bi-bookmark-star me-2" aria-hidden="true" />{result.reservationNotice}</div>
              )}
              <div className="d-flex gap-2 justify-content-center flex-wrap">
                {result.fine && Number(result.fine.outstandingAmount) > 0 && (
                  <button className="btn btn-success" onClick={() => setPaying(result.fine)}><i className="bi bi-cash-coin me-1" />Collect {money(result.fine.outstandingAmount)} now</button>
                )}
                <button className="btn btn-primary" onClick={clear}>{isReturn ? 'Return another book' : 'Renew another book'}</button>
              </div>
            </div>
          </div>
          {result.fine && (
            <div className="col-lg-6">
              <FineSlip title="Fine receipt" dueDate={b.dueDate} returnDate={b.returnDate} overdueDays={result.fine.overdueDays}
                grace={preview?.gracePeriodDays ?? 0} chargeable={preview?.chargeableDays ?? result.fine.overdueDays}
                rate={result.fine.finePerDay} max={preview?.maxFinePerBook} amount={result.fine.amount} capped={preview?.capped}
                status={result.fine.status} paid={result.fine.paidAmount} />
            </div>
          )}
        </div>
        <PaymentModal fine={paying} onClose={() => setPaying(null)}
          onPaid={(p) => { setPaying(null); setResult({ ...result, fine: { ...result.fine, paidAmount: Number(result.fine.paidAmount) + Number(p.paidAmount), outstandingAmount: p.remainingAmount, status: p.paymentStatus === 'PAID' ? 'PAID' : 'PARTIALLY_PAID' } }); }} />
      </>
    );
  }

  /* ------------------------------------------------ choose loan */
  if (!selected) {
    return (
      <>
        <PageHeader title={title} subtitle="Find the loan by member name, member ID, book title or copy code." />
        <input className="form-control form-control-lg mb-3" value={q} onChange={(e) => setQ(e.target.value)} autoFocus
          placeholder="e.g. Riya, STU-2026-001, Clean Code, LIB-CC-001" aria-label="Find borrowed book" />
        <div className="panel p-0">
          {loans.data?.content.length === 0 ? <EmptyState icon="journal-check" title="No borrowed book matches" /> : (
            <div className="list-group list-group-flush">
              {loans.data?.content.map((b) => (
                <button key={b.id} className="list-group-item list-group-item-action d-flex flex-wrap gap-3 align-items-center py-3" onClick={() => setSelected(b)}>
                  <div className="flex-grow-1 text-start">
                    <div className="fw-semibold">{b.bookTitle} <code className="ms-1">{b.copyCode}</code></div>
                    <div className="small text-body-secondary">{b.memberName} · {b.memberCode} · issued {formatDate(b.issueDate)}</div>
                  </div>
                  <DueStamp date={b.dueDate} />
                  <StatusBadge status={b.displayStatus} />
                </button>
              ))}
            </div>
          )}
        </div>
      </>
    );
  }

  /* ------------------------------------------------ selected loan */
  const renewalsLeft = settings ? settings.maxRenewals - selected.renewalCount : null;
  return (
    <>
      <PageHeader title={title} actions={<button className="btn btn-outline-secondary" onClick={clear}><i className="bi bi-arrow-left me-1" />Choose another loan</button>} />
      <div className="row g-3">
        <div className="col-lg-6">
          <div className="panel">
            <h2 className="panel-title">Loan</h2>
            <dl className="detail-list mb-0">
              <dt>Book</dt><dd className="fw-semibold">{selected.bookTitle}</dd>
              <dt>Copy</dt><dd><code>{selected.copyCode}</code></dd>
              <dt>Member</dt><dd><Link to={`/members/${selected.memberId}`}>{selected.memberName}</Link> ({selected.memberCode})</dd>
              <dt>Issued</dt><dd>{formatDate(selected.issueDate)}{selected.issuedBy ? ` by ${selected.issuedBy}` : ''}</dd>
              <dt>Due</dt><dd><DueStamp date={selected.dueDate} /></dd>
              <dt>Renewals</dt><dd>{selected.renewalCount}{settings ? ` of ${settings.maxRenewals} used` : ''}</dd>
            </dl>
          </div>
          {isReturn && (
            <div className="panel mt-3">
              <h2 className="panel-title">Return details</h2>
              <div className="row g-3">
                <div className="col-sm-6">
                  <label className="form-label" htmlFor="returnDate">Return date</label>
                  <input id="returnDate" type="date" className="form-control" value={returnDate} max={todayIso()} min={selected.issueDate}
                    onChange={(e) => setReturnDate(e.target.value)} />
                  <div className="form-text">Change only when recording a book dropped in the return box earlier.</div>
                </div>
                <div className="col-sm-6">
                  <label className="form-label" htmlFor="condition">Condition</label>
                  <select id="condition" className="form-select" value={condition} onChange={(e) => setCondition(e.target.value)}>
                    <option value="GOOD">Good</option>
                    <option value="DAMAGED">Damaged</option>
                    <option value="LOST">Lost</option>
                  </select>
                  <div className="form-text">{{ GOOD: 'Goes back on the shelf (or to the next reservation).', DAMAGED: 'Set aside for repair; not issued until fixed.', LOST: 'Copy marked lost and removed from stock.' }[condition]}</div>
                </div>
                <div className="col-12">
                  <label className="form-label" htmlFor="remarks">Remarks (optional)</label>
                  <input id="remarks" className="form-control" value={remarks} onChange={(e) => setRemarks(e.target.value)} />
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="col-lg-6">
          {isReturn ? (
            <>
              {preview && (
                <FineSlip title="Fine calculation" dueDate={preview.borrowing.dueDate} returnDate={preview.returnDate} overdueDays={preview.overdueDays}
                  grace={preview.gracePeriodDays} chargeable={preview.chargeableDays} rate={preview.finePerDay} max={preview.maxFinePerBook}
                  amount={preview.fineAmount} capped={preview.capped} previousOwed={preview.memberOutstandingFine} />
              )}
              {error && <div className="alert alert-danger mt-3" role="alert">{error}</div>}
              <button className="btn btn-primary btn-lg w-100 mt-3" disabled={busy || !preview} onClick={confirmReturn}>
                {busy && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                Confirm return{preview && Number(preview.fineAmount) > 0 ? ` and add ${money(preview.fineAmount)} fine` : ''}
              </button>
            </>
          ) : (
            <div className="panel">
              <h2 className="panel-title">Renewal</h2>
              {settings && (
                <ul className="rule-list">
                  <li className={renewalsLeft > 0 ? 'ok' : 'no'}>{renewalsLeft > 0 ? `${renewalsLeft} renewal${renewalsLeft === 1 ? '' : 's'} left` : 'Maximum renewals reached'}</li>
                  <li className={selected.displayStatus !== 'OVERDUE' || settings.allowRenewalWhenOverdue ? 'ok' : 'no'}>
                    {selected.displayStatus === 'OVERDUE' ? (settings.allowRenewalWhenOverdue ? 'Overdue, but overdue renewals are allowed' : 'Overdue books cannot be renewed') : 'Not overdue'}</li>
                  <li className="info">Books reserved by another member cannot be renewed (checked on confirm)</li>
                  <li className="info">Extends the due date by {settings.renewalPeriodDays} days</li>
                </ul>
              )}
              {error && <div className="alert alert-danger" role="alert">{error}</div>}
              <button className="btn btn-primary btn-lg w-100" disabled={busy} onClick={renew}>
                {busy && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}Renew loan
              </button>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

/** The fine breakdown, laid out like a library receipt slip. */
function FineSlip({ title, dueDate, returnDate, overdueDays, grace, chargeable, rate, max, amount, capped, status, paid, previousOwed }) {
  const noFine = Number(amount) === 0;
  return (
    <section className="fine-slip" aria-label={title}>
      <header><span>{title}</span>{status && <StatusBadge status={status} />}</header>
      <div className="slip-row"><span>Due date</span><span>{formatDate(dueDate)}</span></div>
      <div className="slip-row"><span>Returned</span><span>{formatDate(returnDate)}</span></div>
      <div className="slip-row"><span>Days late</span><span>{overdueDays}</span></div>
      {grace > 0 && <div className="slip-row"><span>Grace period</span><span>− {grace} days</span></div>}
      <div className="slip-row"><span>Chargeable days × rate</span><span>{chargeable} × {money(rate)}</span></div>
      {capped && max && <div className="slip-row text-danger"><span>Capped at maximum</span><span>{money(max)}</span></div>}
      <div className={`slip-total ${noFine ? 'zero' : ''}`}><span>Fine</span><span>{money(amount)}</span></div>
      {noFine && <div className="slip-stamp">No fine</div>}
      {paid != null && Number(paid) > 0 && <div className="slip-row"><span>Paid</span><span>{money(paid)}</span></div>}
      {previousOwed != null && Number(previousOwed) > 0 && (
        <div className="slip-note">This member already owes {money(previousOwed)} from earlier fines.</div>
      )}
    </section>
  );
}
