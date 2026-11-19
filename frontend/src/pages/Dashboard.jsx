import { useEffect, useState } from 'react';
import { NavLink } from 'react-router-dom';
import api, { readError } from '../api.js';
import { useAuth } from '../AuthContext.jsx';
import { ROLE_LABEL, ROLE_NAV } from '../roles.js';

const STAT_LABELS = { totalUsers: 'Total users', tenants: 'Tenants', owners: 'Owners', admins: 'Admins', activeUsers: 'Active users' };

/** One dashboard component for all roles: navigation comes from ROLE_NAV, data from /api/{role}/dashboard. */
export default function Dashboard({ role }) {
  const { user, logout } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get(`/api/${role.toLowerCase()}/dashboard`)
      .then((res) => setData(res.data))
      .catch((err) => setError(readError(err).message));
  }, [role]);

  return (
    <div className="shell">
      <header className="topbar">
        <strong>HomeSeek</strong>
        <span className="muted">{user.fullName} · <span className="badge">{ROLE_LABEL[role]}</span></span>
        <button className="btn secondary small" onClick={logout}>Log out</button>
      </header>
      <div className="body">
        <nav aria-label={`${ROLE_LABEL[role]} navigation`}>
          <ul>
            {ROLE_NAV[role].map((item) => (
              <li key={item.label}>
                {item.to
                  ? <NavLink to={item.to} end>{item.label}</NavLink>
                  : <span className="disabled" aria-disabled="true" title="Coming in a later increment">{item.label}</span>}
              </li>
            ))}
          </ul>
        </nav>
        <main>
          <h1>{ROLE_LABEL[role]} Dashboard</h1>
          {error && <p className="error banner" role="alert">{error}</p>}
          {!data && !error && <p className="muted">Loading...</p>}
          {data && (
            <>
              <p>{data.welcome}</p>
              {data.stats && (
                <section aria-label="Summary">
                  <div className="stats">
                    {Object.entries(data.stats).map(([key, value]) => (
                      <div className="stat" key={key}><span className="value">{value}</span><span className="muted">{STAT_LABELS[key] || key}</span></div>
                    ))}
                  </div>
                </section>
              )}
              <h2>Features for your role</h2>
              <ul className="modules">
                {data.modules.map((m) => <li key={m}>{m} <span className="muted">(coming soon)</span></li>)}
              </ul>
            </>
          )}
        </main>
      </div>
    </div>
  );
}
