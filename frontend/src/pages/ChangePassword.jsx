import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { readError } from '../api.js';
import { useAuth } from '../AuthContext.jsx';
import { rules } from '../validation.js';
import Shell from '../components/Shell.jsx';
import Field from '../components/Field.jsx';

const EMPTY = { currentPassword: '', newPassword: '', confirm: '' };

/** Change the logged-in user's password. The server checks the current password and ends every older session. */
export default function ChangePassword() {
  const { user, login, logout } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);

  const set = (e) => { setForm({ ...form, [e.target.name]: e.target.value }); setDone(false); };

  async function submit(e) {
    e.preventDefault();
    setError('');
    setDone(false);
    const found = {};
    if (!form.currentPassword) found.currentPassword = 'Current password is required';
    const strength = rules.password(form.newPassword);
    if (strength) found.newPassword = strength;
    else if (form.newPassword === form.currentPassword) found.newPassword = 'New password must be different from the current password';
    if (form.confirm !== form.newPassword) found.confirm = 'Passwords do not match';
    setErrors(found);
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      await api.post('/api/password/change', { currentPassword: form.currentPassword, newPassword: form.newPassword });
    } catch (err) {
      const { message, fieldErrors } = readError(err);
      setErrors(fieldErrors);
      setError(Object.keys(fieldErrors).length ? '' : message);
      setBusy(false);
      return;
    }
    // The old token stopped working with the old password, so sign in again with the new one to get a fresh session.
    try {
      await login(user.email, form.newPassword);
      setForm(EMPTY);
      setDone(true);
    } catch {
      await logout();
      navigate('/login', { state: { reset: true, email: user.email } });
    } finally {
      setBusy(false);
    }
  }

  return (
    <Shell>
      <h1>Change Password</h1>
      <form className="card-form" onSubmit={submit} noValidate>
        <Field id="currentPassword" label="Current password" type="password" autoComplete="current-password"
          value={form.currentPassword} onChange={set} error={errors.currentPassword} />
        <Field id="newPassword" label="New password" type="password" autoComplete="new-password" value={form.newPassword}
          onChange={set} error={errors.newPassword} hint="At least 8 characters, with a letter and a number" />
        <Field id="confirm" label="Confirm new password" type="password" autoComplete="new-password" value={form.confirm}
          onChange={set} error={errors.confirm} />
        {error && <p className="error banner" role="alert">{error}</p>}
        {done && <p className="notice" role="status">Password changed. Your other sessions have been signed out.</p>}
        <button className="btn" type="submit" disabled={busy}>{busy ? 'Saving...' : 'Change password'}</button>
      </form>
    </Shell>
  );
}
