import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';
import { ROLE_LABEL, ROLE_NAV } from '../roles.js';

/** Top bar and role-based side navigation shared by the dashboard and the account pages. */
export default function Shell({ children }) {
  const { user, logout } = useAuth();
  const role = user.role;
  return (
    <div className="shell">
      <header className="topbar">
        <strong>HomeSeek</strong>
        <span className="muted"><Link to="/profile">{user.fullName}</Link> · <span className="badge">{ROLE_LABEL[role]}</span></span>
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
        <main>{children}</main>
      </div>
    </div>
  );
}
