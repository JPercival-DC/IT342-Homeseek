import { useEffect, useState } from 'react';
import api, { readError } from '../api.js';
import { useAuth } from '../AuthContext.jsx';
import { check } from '../validation.js';
import { ROLE_LABEL } from '../roles.js';
import Shell from '../components/Shell.jsx';
import Field from '../components/Field.jsx';

/** FR-008: view and edit the logged-in user's own profile. There is no user id anywhere: the server uses the token. */
export default function Profile() {
  const { setUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ fullName: '', phone: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.get('/api/profile')
      .then((res) => {
        setProfile(res.data);
        setForm({ fullName: res.data.fullName, phone: res.data.phone || '' });
      })
      .catch((err) => setError(readError(err).message));
  }, []);

  const set = (e) => { setForm({ ...form, [e.target.name]: e.target.value }); setSaved(false); };

  async function submit(e) {
    e.preventDefault();
    setError('');
    setSaved(false);
    const found = check(form, ['fullName', 'phone']);
    setErrors(found);
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      const res = await api.put('/api/profile', form);
      setProfile(res.data);
      setUser(res.data);              // keeps the name in the top bar current
      setSaved(true);
    } catch (err) {
      const { message, fieldErrors } = readError(err);
      setErrors(fieldErrors);
      setError(Object.keys(fieldErrors).length ? '' : message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Shell>
      <h1>My Profile</h1>
      {!profile && !error && <p className="muted">Loading...</p>}
      {!profile && error && <p className="error banner" role="alert">{error}</p>}
      {profile && (
        <form className="card-form" onSubmit={submit} noValidate>
          <Field id="email" label="Email" value={profile.email} readOnly hint="Your email is your login and cannot be changed here" />
          <p className="muted">Role: <span className="badge">{ROLE_LABEL[profile.role]}</span></p>
          <Field id="fullName" label="Full name" autoComplete="name" value={form.fullName} onChange={set} error={errors.fullName} />
          <Field id="phone" label="Phone number" type="tel" autoComplete="tel" value={form.phone} onChange={set} error={errors.phone} />
          {error && <p className="error banner" role="alert">{error}</p>}
          {saved && <p className="notice" role="status">Profile updated.</p>}
          <button className="btn" type="submit" disabled={busy}>{busy ? 'Saving...' : 'Save changes'}</button>
        </form>
      )}
    </Shell>
  );
}
