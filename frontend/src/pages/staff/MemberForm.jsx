import { useEffect, useState } from 'react';
import api, { errorMessage, fieldErrors } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';
import { Field, Modal } from '../../components/Common.jsx';
import { blanksToNull, todayIso } from '../../utils/format.js';

const EMPTY = { memberCode: '', fullName: '', email: '', phone: '', department: '', course: '', yearOfStudy: '', address: '',
  membershipDate: todayIso(), maxBooksAllowed: '', username: '', password: '' };

export default function MemberForm({ show, member, onClose, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!show) return;
    setErrors({});
    setForm(member ? { ...EMPTY, ...Object.fromEntries(Object.entries(member).map(([k, v]) => [k, v ?? ''])),
      maxBooksAllowed: member.customLimit ? member.maxBooksAllowed : '', username: '', password: '' } : { ...EMPTY, membershipDate: todayIso() });
  }, [show, member]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const input = (name, props = {}) => <input id={name} className={`form-control ${errors[name] ? 'is-invalid' : ''}`} value={form[name]} onChange={set(name)} {...props} />;

  const save = async () => {
    setBusy(true); setErrors({});
    const body = blanksToNull({
      memberCode: form.memberCode, fullName: form.fullName, email: form.email, phone: form.phone, department: form.department,
      course: form.course, yearOfStudy: form.yearOfStudy === '' ? '' : Number(form.yearOfStudy), address: form.address,
      membershipDate: form.membershipDate, maxBooksAllowed: form.maxBooksAllowed === '' ? '' : Number(form.maxBooksAllowed),
      username: member ? '' : form.username, password: member ? '' : form.password,
    });
    try {
      const res = member ? await api.put(`/members/${member.id}`, body) : await api.post('/members', body);
      toast.success(member ? 'Member updated' : `${res.data.fullName} registered`);
      onSaved(res.data);
    } catch (e) { setErrors(fieldErrors(e)); toast.error(errorMessage(e)); } finally { setBusy(false); }
  };

  return (
    <Modal show={show} title={member ? 'Edit member' : 'Add member'} onClose={onClose} size="lg"
      footer={<><button className="btn btn-light" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={save} disabled={busy}>{busy && <span className="spinner-border spinner-border-sm me-2" />}Save</button></>}>
      <div className="row g-3">
        <Field label="Full name" name="fullName" error={errors.fullName} className="col-md-8">{input('fullName')}</Field>
        <Field label="Member ID" name="memberCode" error={errors.memberCode} className="col-md-4">{input('memberCode', { placeholder: 'STU-2026-011' })}</Field>
        <Field label="Email" name="email" error={errors.email}>{input('email', { type: 'email' })}</Field>
        <Field label="Phone" name="phone" error={errors.phone}>{input('phone', { type: 'tel' })}</Field>
        <Field label="Department" name="department" error={errors.department}>{input('department')}</Field>
        <Field label="Course" name="course" error={errors.course} className="col-md-3">{input('course')}</Field>
        <Field label="Year" name="yearOfStudy" error={errors.yearOfStudy} className="col-md-3">{input('yearOfStudy', { type: 'number', min: 1, max: 8 })}</Field>
        <Field label="Address" name="address" error={errors.address} className="col-12">{input('address')}</Field>
        <Field label="Membership date" name="membershipDate" error={errors.membershipDate}>{input('membershipDate', { type: 'date' })}</Field>
        <Field label="Borrowing limit" name="maxBooksAllowed" error={errors.maxBooksAllowed} hint="Leave empty to use the library default">{input('maxBooksAllowed', { type: 'number', min: 1, max: 20 })}</Field>
        {!member && (
          <>
            <div className="col-12"><hr className="my-1" /><div className="small fw-semibold">Portal login (optional)</div></div>
            <Field label="Username" name="username" error={errors.username}>{input('username', { autoComplete: 'off' })}</Field>
            <Field label="Temporary password" name="password" error={errors.password} hint="8+ characters with a letter and a number">{input('password', { type: 'password', autoComplete: 'new-password' })}</Field>
          </>
        )}
      </div>
    </Modal>
  );
}
