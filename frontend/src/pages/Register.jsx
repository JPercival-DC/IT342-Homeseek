import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { readError } from '../api.js';
import Field from '../components/Field.jsx';

// Same rules as the server (FR-001, FR-002). The server is the final authority.
function validate(f) {
  const errors = {};
  if (f.fullName.trim().length < 2 || f.fullName.trim().length > 100) errors.fullName = 'Full name must be 2 to 100 characters';
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f.email.trim())) errors.email = 'Enter a valid email address';
  if (!/^\+?[0-9][0-9 -]{6,18}[0-9]$/.test(f.phone.trim())) errors.phone = 'Enter a valid phone number (8 to 20 digits)';
  if (f.password.length < 8 || f.password.length > 72) errors.password = 'Password must be 8 to 72 characters';
  else if (!/[A-Za-z]/.test(f.password) || !/\d/.test(f.password)) errors.password = 'Password must contain at least one letter and one number';
  if (f.confirm !== f.password) errors.confirm = 'Passwords do not match';
  return errors;
}

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '', confirm: '', role: 'TENANT' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setError('');
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      const { fullName, email, phone, password, role } = form;
      await api.post('/api/auth/register', { fullName, email, phone, password, role });
      navigate('/login', { state: { registered: true, email: email.trim() } });
    } catch (err) {
      const { message, fieldErrors } = readError(err);
      setErrors(fieldErrors);
      setError(Object.keys(fieldErrors).length ? '' : message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="card narrow">
      <h1>Create account</h1>
      <form onSubmit={submit} noValidate>
        <Field id="fullName" label="Full name" autoComplete="name" value={form.fullName} onChange={set} error={errors.fullName} />
        <Field id="email" label="Email" type="email" autoComplete="email" value={form.email} onChange={set} error={errors.email} />
        <Field id="phone" label="Phone number" type="tel" autoComplete="tel" value={form.phone} onChange={set} error={errors.phone} />
        <Field id="password" label="Password" type="password" autoComplete="new-password" value={form.password} onChange={set}
          error={errors.password} hint="At least 8 characters, with a letter and a number" />
        <Field id="confirm" label="Confirm password" type="password" autoComplete="new-password" value={form.confirm} onChange={set} error={errors.confirm} />
        <div className="field">
          <label htmlFor="role">I am a</label>
          <select id="role" name="role" value={form.role} onChange={set}>
            <option value="TENANT">Tenant (looking for a room)</option>
            <option value="OWNER">Owner (renting out rooms)</option>
          </select>
          {errors.role && <small className="error" role="alert">{errors.role}</small>}
        </div>
        {error && <p className="error banner" role="alert">{error}</p>}
        <button className="btn" type="submit" disabled={busy}>{busy ? 'Creating account...' : 'Create account'}</button>
      </form>
      <p className="muted">Already registered? <Link to="/login">Log in</Link></p>
    </main>
  );
}
