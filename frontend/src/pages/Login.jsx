import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';
import { readError } from '../api.js';
import { ROLE_HOME } from '../roles.js';
import Field from '../components/Field.jsx';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: location.state?.email || '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setError('');
    if (!form.email.trim() || !form.password) {
      setError('Enter your email and password.');
      return;
    }
    setBusy(true);
    try {
      const user = await login(form.email.trim(), form.password);
      navigate(ROLE_HOME[user.role], { replace: true }); // each role lands on its own dashboard
    } catch (err) {
      setError(readError(err).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="card narrow">
      <h1>Log in</h1>
      {location.state?.reset && (
        <p className="notice" role="status">Password updated. Log in with your new password.</p>
      )}
      {location.state?.registered && (
        <p className="notice" role="status">Account created. You can now log in.</p>
      )}
      <form onSubmit={submit} noValidate>
        <Field id="email" label="Email" type="email" autoComplete="email" value={form.email} onChange={set} />
        <Field id="password" label="Password" type="password" autoComplete="current-password" value={form.password} onChange={set} />
        {error && <p className="error banner" role="alert">{error}</p>}
        <button className="btn" type="submit" disabled={busy}>{busy ? 'Logging in...' : 'Log in'}</button>
      </form>
      <p className="muted"><Link to="/forgot-password">Forgot password?</Link></p>
      <p className="muted">No account yet? <Link to="/register">Create one</Link></p>
    </main>
  );
}
