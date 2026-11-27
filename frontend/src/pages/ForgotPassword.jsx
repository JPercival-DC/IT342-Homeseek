import { useState } from 'react';
import { Link } from 'react-router-dom';
import api, { readError } from '../api.js';
import { rules } from '../validation.js';
import Field from '../components/Field.jsx';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setError('');
    const problem = rules.email(email);
    if (problem) { setError(problem); return; }
    setBusy(true);
    try {
      await api.post('/api/password/forgot', { email: email.trim() });
      setSent(true);
    } catch (err) {
      setError(readError(err).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="card narrow">
      <h1>Forgot password</h1>
      {sent ? (
        <p className="notice" role="status">
          If an account exists for {email.trim()}, we have emailed a link to reset the password. The link works once and
          expires in 30 minutes. Check your spam folder if it does not arrive.
        </p>
      ) : (
        <form onSubmit={submit} noValidate>
          <p className="muted">Enter the email you registered with and we will send you a reset link.</p>
          <Field id="email" label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          {error && <p className="error banner" role="alert">{error}</p>}
          <button className="btn" type="submit" disabled={busy}>{busy ? 'Sending...' : 'Send reset link'}</button>
        </form>
      )}
      <p className="muted"><Link to="/login">Back to log in</Link></p>
    </main>
  );
}
