import { useEffect, useState } from 'react';
import api, { errorMessage, fieldErrors } from '../api/client.js';
import { useToast } from '../context/ToastContext.jsx';
import { Field, Modal } from './Common.jsx';
import { money } from '../utils/format.js';

/**
 * Records a fine payment received at the counter. This is a mock / offline payment:
 * no card is charged and no gateway is contacted.
 */
export default function PaymentModal({ fine, onClose, onPaid }) {
  const toast = useToast();
  const [form, setForm] = useState({ amount: '', paymentMethod: 'CASH', referenceNumber: '', remarks: '' });
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (fine) { setForm({ amount: fine.outstandingAmount, paymentMethod: 'CASH', referenceNumber: '', remarks: '' }); setErrors({}); }
  }, [fine]);

  const needsRef = form.paymentMethod === 'UPI' || form.paymentMethod === 'CARD';
  const amount = Number(form.amount);
  const tooMuch = fine && amount > Number(fine.outstandingAmount);

  const submit = async () => {
    setBusy(true); setErrors({});
    try {
      const res = await api.post(`/fines/${fine.id}/payment`, {
        amount, paymentMethod: form.paymentMethod,
        referenceNumber: form.referenceNumber || null, remarks: form.remarks || null,
      });
      toast.success(Number(res.data.remainingAmount) === 0 ? `Fine fully paid (${money(res.data.paidAmount)})`
        : `Recorded ${money(res.data.paidAmount)}. ${money(res.data.remainingAmount)} still due.`);
      onPaid(res.data);
    } catch (e) { setErrors(fieldErrors(e)); toast.error(errorMessage(e)); } finally { setBusy(false); }
  };

  return (
    <Modal show={!!fine} title="Record fine payment" onClose={onClose}
      footer={<>
        <button className="btn btn-light" onClick={onClose}>Cancel</button>
        <button className="btn btn-success" onClick={submit} disabled={busy || !amount || amount <= 0 || tooMuch || (needsRef && !form.referenceNumber)}>
          {busy && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}Record {amount > 0 ? money(amount) : 'payment'}
        </button>
      </>}>
      {fine && (
        <>
          <div className="fine-summary mb-3">
            <div><span>Member</span><strong>{fine.memberName} ({fine.memberCode})</strong></div>
            <div><span>Book</span><strong>{fine.bookTitle}</strong></div>
            <div><span>Fine</span><strong>{money(fine.amount)} for {fine.overdueDays} days late</strong></div>
            <div><span>Already paid</span><strong>{money(fine.paidAmount)}</strong></div>
            <div className="total"><span>Outstanding</span><strong>{money(fine.outstandingAmount)}</strong></div>
          </div>
          <div className="row g-3">
            <Field label="Amount received (₹)" name="amount" error={errors.amount || (tooMuch ? 'More than the outstanding amount' : null)}
              hint="A smaller amount is recorded as a part payment">
              <input id="amount" type="number" step="0.01" min="0.01" className={`form-control ${tooMuch ? 'is-invalid' : ''}`}
                value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
            </Field>
            <Field label="Method" name="paymentMethod">
              <select id="paymentMethod" className="form-select" value={form.paymentMethod} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}>
                <option value="CASH">Cash</option><option value="UPI">UPI</option><option value="CARD">Card</option><option value="OTHER">Other</option>
              </select>
            </Field>
            <Field label={`Reference number${needsRef ? '' : ' (optional)'}`} name="referenceNumber" error={errors.referenceNumber}
              className="col-12" hint={needsRef ? 'UPI transaction ID or card slip number' : undefined}>
              <input id="referenceNumber" className="form-control" value={form.referenceNumber} onChange={(e) => setForm({ ...form, referenceNumber: e.target.value })} />
            </Field>
            <Field label="Remarks (optional)" name="remarks" className="col-12">
              <input id="remarks" className="form-control" value={form.remarks} onChange={(e) => setForm({ ...form, remarks: e.target.value })} />
            </Field>
          </div>
          <p className="small text-body-secondary mt-3 mb-0"><i className="bi bi-info-circle me-1" aria-hidden="true" />Offline payment record — no real transaction is processed.</p>
        </>
      )}
    </Modal>
  );
}
