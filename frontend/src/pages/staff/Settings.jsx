import { useEffect, useState } from 'react';
import api, { errorMessage, fieldErrors } from '../../api/client.js';
import { useApi } from '../../hooks/useApi.js';
import { useToast } from '../../context/ToastContext.jsx';
import { ErrorAlert, Field, Loading, PageHeader } from '../../components/Common.jsx';
import { formatDateTime, money } from '../../utils/format.js';

/** Same formula as the backend FineCalculator, used only to preview the effect of the settings. */
function previewFine(daysLate, s) {
  const chargeable = Math.max(0, daysLate - Number(s.gracePeriodDays || 0));
  return Math.min(chargeable * Number(s.finePerDay || 0), Number(s.maxFinePerBook || 0));
}

export default function Settings() {
  const toast = useToast();
  const { data, loading, error, reload } = useApi('/settings');
  const [form, setForm] = useState(null);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [daysLate, setDaysLate] = useState(5);

  useEffect(() => { if (data) setForm(data); }, [data]);
  if (loading && !form) return <Loading />;
  if (error) return <ErrorAlert message={error} onRetry={reload} />;
  if (!form) return null;

  const num = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const bool = (k) => (e) => setForm({ ...form, [k]: e.target.checked });

  const save = async (e) => {
    e.preventDefault();
    setBusy(true); setErrors({});
    const body = { ...form };
    ['gracePeriodDays', 'maxBooksPerMember', 'loanPeriodDays', 'maxRenewals', 'renewalPeriodDays', 'reservationHoldDays', 'dueReminderDays']
      .forEach((k) => { body[k] = Number(body[k]); });
    ['finePerDay', 'maxFinePerBook', 'fineBlockThreshold'].forEach((k) => { body[k] = Number(body[k]); });
    try { const res = await api.put('/settings', body); setForm(res.data); toast.success('Settings saved. New rules apply from now on.'); }
    catch (err) { setErrors(fieldErrors(err)); toast.error(errorMessage(err)); } finally { setBusy(false); }
  };

  const numInput = (k, props = {}) => <input id={k} type="number" className={`form-control ${errors[k] ? 'is-invalid' : ''}`} value={form[k]} onChange={num(k)} {...props} />;

  return (
    <>
      <PageHeader title="Library settings" subtitle={form.updatedAt ? `Last changed ${formatDateTime(form.updatedAt)} by ${form.updatedBy || 'system'}` : undefined} />
      <form onSubmit={save} className="row g-3">
        <div className="col-xl-8">
          <div className="panel mb-3">
            <h2 className="panel-title">Fines</h2>
            <div className="row g-3">
              <Field label="Fine per day (₹)" name="finePerDay" error={errors.finePerDay} className="col-md-4">{numInput('finePerDay', { step: '0.50', min: 0 })}</Field>
              <Field label="Maximum fine per book (₹)" name="maxFinePerBook" error={errors.maxFinePerBook} className="col-md-4">{numInput('maxFinePerBook', { step: '1', min: 0 })}</Field>
              <Field label="Grace period (days)" name="gracePeriodDays" error={errors.gracePeriodDays} className="col-md-4" hint="Late days that are not charged">{numInput('gracePeriodDays', { min: 0 })}</Field>
              <div className="col-md-8">
                <div className="form-check form-switch">
                  <input id="blockIssue" className="form-check-input" type="checkbox" checked={!!form.blockIssueOnUnpaidFines} onChange={bool('blockIssueOnUnpaidFines')} />
                  <label className="form-check-label" htmlFor="blockIssue">Block new issues when a member owes more than the limit</label>
                </div>
              </div>
              <Field label="Unpaid fine limit (₹)" name="fineBlockThreshold" error={errors.fineBlockThreshold} className="col-md-4">{numInput('fineBlockThreshold', { min: 0, disabled: !form.blockIssueOnUnpaidFines })}</Field>
            </div>
            <p className="small text-body-secondary mt-3 mb-0">Changes apply to books returned from now on. Existing fines keep the rate they were created with.</p>
          </div>
          <div className="panel mb-3">
            <h2 className="panel-title">Borrowing and renewals</h2>
            <div className="row g-3">
              <Field label="Books per member" name="maxBooksPerMember" error={errors.maxBooksPerMember} className="col-md-4" hint="Default; can be changed per member">{numInput('maxBooksPerMember', { min: 1 })}</Field>
              <Field label="Loan period (days)" name="loanPeriodDays" error={errors.loanPeriodDays} className="col-md-4">{numInput('loanPeriodDays', { min: 1 })}</Field>
              <Field label="Due reminder (days before)" name="dueReminderDays" error={errors.dueReminderDays} className="col-md-4">{numInput('dueReminderDays', { min: 0 })}</Field>
              <Field label="Maximum renewals" name="maxRenewals" error={errors.maxRenewals} className="col-md-4">{numInput('maxRenewals', { min: 0 })}</Field>
              <Field label="Days added per renewal" name="renewalPeriodDays" error={errors.renewalPeriodDays} className="col-md-4">{numInput('renewalPeriodDays', { min: 1 })}</Field>
              <div className="col-md-4 d-flex align-items-end">
                <div className="form-check form-switch">
                  <input id="overdueRenew" className="form-check-input" type="checkbox" checked={!!form.allowRenewalWhenOverdue} onChange={bool('allowRenewalWhenOverdue')} />
                  <label className="form-check-label" htmlFor="overdueRenew">Allow renewing overdue books</label>
                </div>
              </div>
            </div>
          </div>
          <div className="panel mb-3">
            <h2 className="panel-title">Reservations</h2>
            <div className="row g-3">
              <Field label="Hold a returned copy for (days)" name="reservationHoldDays" error={errors.reservationHoldDays} className="col-md-4"
                hint="Then the hold expires and the next member gets it">{numInput('reservationHoldDays', { min: 1 })}</Field>
            </div>
          </div>
          <button className="btn btn-primary btn-lg" disabled={busy}>{busy && <span className="spinner-border spinner-border-sm me-2" />}Save settings</button>
        </div>
        <div className="col-xl-4">
          <div className="panel position-sticky" style={{ top: '1rem' }}>
            <h2 className="panel-title">Fine calculator preview</h2>
            <label className="form-label" htmlFor="daysLate">If a book is returned this many days late</label>
            <input id="daysLate" type="range" className="form-range" min={0} max={150} value={daysLate} onChange={(e) => setDaysLate(Number(e.target.value))} />
            <div className="fine-summary">
              <div><span>Days late</span><strong>{daysLate}</strong></div>
              <div><span>Chargeable (minus grace)</span><strong>{Math.max(0, daysLate - Number(form.gracePeriodDays || 0))}</strong></div>
              <div className="total"><span>Fine</span><strong>{money(previewFine(daysLate, form))}</strong></div>
            </div>
            <p className="small text-body-secondary mt-3 mb-0">fine = min((days late − grace) × rate, maximum)</p>
          </div>
        </div>
      </form>
    </>
  );
}
