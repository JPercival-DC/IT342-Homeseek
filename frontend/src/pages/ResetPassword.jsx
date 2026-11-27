import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { readError } from '../api.js';
import { rules } from '../validation.js';
import Field from '../components/Field.jsx';

/**
 * Opened from the link in the reset email: /reset-password?token=...
 * The token is kept in memory and removed from the address bar so it does not stay in history or leak in a Referer header.
 */
export default function ResetPassword() {
  const navigate = useNavigate();
  const [token] = useState(() => new URLSearchParams(window.location.search).get('token'));
  const [form, setForm] = useState({ newPassword: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { window.history.replaceState(null, '', window.location.pathname); }, []);

  const set = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setError('');
    const found = {};
    const strength = rules.password(form.newPassword);
    if (strength) found.newPassword = strength;
    if (form.confirm !== form.newPassword) found.confirm = 'Passwords do not match';
    setErrors(found);
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      await api.post('/api/password/reset', { token, newPassword: form.newPassword });
      navigate('/login', { state: { reset: true } });
    } catch (err) {
      const { message, fieldErrors } = readError(err);
      setErrors(fieldErrors);
      setError(Object.keys(fieldErrors).length ? '' : message);
    } finally {
      setBusy(false);
    }
  }

  if (!token) {
    return (
      <main className="card narrow">
        <h1>Reset password</h1>
        <p className="error banner" role="alert">This reset link is not valid. Please request a new one.</p>
        <Link className="btn" to="/forgot-password">Request a new link</Link>
      </main>
    );
  }

  return (
    <main className="card narrow">
      <h1>Set a new password</h1>
      <form onSubmit={submit} noValidate>
        <Field id="newPassword" label="New password" type="password" autoComplete="new-password" value={form.newPassword}
          onChange={set} error={errors.newPassword} hint="At least 8 characters, with a letter and a number" />
        <Field id="confirm" label="Confirm new password" type="password" autoComplete="new-password" value={form.confirm}
          onChange={set} error={errors.confirm} />
        {error && (
          <p className="error banner" role="alert">
            {error} {error.includes('reset link') && <Link to="/forgot-password">Request a new link</Link>}
          </p>
        )}
        <button className="btn" type="submit" disabled={busy}>{busy ? 'Saving...' : 'Reset password'}</button>
      </form>
    </main>
  );
}
