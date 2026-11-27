import { useEffect, useState } from 'react';
import api, { readError } from '../api.js';
import { ROLE_LABEL } from '../roles.js';
import Shell from '../components/Shell.jsx';

const STAT_LABELS = { totalUsers: 'Total users', tenants: 'Tenants', owners: 'Owners', admins: 'Admins', activeUsers: 'Active users' };

/** One dashboard component for all roles: navigation comes from ROLE_NAV, data from /api/{role}/dashboard. */
export default function Dashboard({ role }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get(`/api/${role.toLowerCase()}/dashboard`)
      .then((res) => setData(res.data))
      .catch((err) => setError(readError(err).message));
  }, [role]);

  return (
    <Shell>
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
    </Shell>
  );
}
