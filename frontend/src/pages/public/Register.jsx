import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { errorMessage, fieldErrors } from '../../api/client.js';
import { blanksToNull } from '../../utils/format.js';
import { Field } from '../../components/Common.jsx';
import AuthShell from './AuthShell.jsx';

const EMPTY = { fullName: '', memberCode: '', email: '', username: '', password: '', phone: '', department: '', course: '', yearOfStudy: '' };

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError(''); setErrors({});
    try {
      const body = blanksToNull({ ...form, yearOfStudy: form.yearOfStudy ? Number(form.yearOfStudy) : '' });
      await register(body);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const input = (name, props = {}) => (
    <input id={name} className={`form-control ${errors[name] ? 'is-invalid' : ''}`} value={form[name]} onChange={set(name)} {...props} />
  );

  return (
    <AuthShell title="Create a library account" subtitle="You can borrow books once your account is set up.">
      {error && <div className="alert alert-danger py-2" role="alert">{error}</div>}
      <form onSubmit={submit} className="row g-3" noValidate>
        <Field label="Full name" name="fullName" error={errors.fullName} className="col-12">{input('fullName', { required: true, autoComplete: 'name' })}</Field>
        <Field label="Student / employee ID" name="memberCode" error={errors.memberCode} hint="As printed on your college ID card">{input('memberCode', { required: true })}</Field>
        <Field label="College email" name="email" error={errors.email}>{input('email', { type: 'email', required: true, autoComplete: 'email' })}</Field>
        <Field label="Username" name="username" error={errors.username}>{input('username', { required: true, autoComplete: 'username' })}</Field>
        <Field label="Password" name="password" error={errors.password} hint="8+ characters, with a letter and a number">{input('password', { type: 'password', required: true, autoComplete: 'new-password' })}</Field>
        <Field label="Department" name="department" error={errors.department}>{input('department')}</Field>
        <Field label="Course" name="course" error={errors.course} className="col-md-3">{input('course')}</Field>
        <Field label="Year" name="yearOfStudy" error={errors.yearOfStudy} className="col-md-3">
          <select id="yearOfStudy" className="form-select" value={form.yearOfStudy} onChange={set('yearOfStudy')}>
            <option value="">—</option>{[1, 2, 3, 4, 5, 6].map((y) => <option key={y} value={y}>{y}</option>)}
          </select>
        </Field>
        <Field label="Phone" name="phone" error={errors.phone} className="col-12">{input('phone', { type: 'tel', autoComplete: 'tel' })}</Field>
        <div className="col-12">
          <button className="btn btn-primary btn-lg w-100" disabled={busy}>
            {busy && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}Create account
          </button>
        </div>
      </form>
      <p className="mt-4 text-center">Already registered? <Link to="/login">Log in</Link></p>
    </AuthShell>
  );
}
