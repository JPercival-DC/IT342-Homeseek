import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';
import { ROLE_HOME } from '../roles.js';

export default function Home() {
  const { user } = useAuth();
  return (
    <main className="card narrow center">
      <h1>HomeSeek</h1>
      <p className="muted">Verified boarding houses and rooms in Cebu City, in one place.</p>
      {user ? (
        <Link className="btn" to={ROLE_HOME[user.role]}>Go to my dashboard</Link>
      ) : (
        <div className="row">
          <Link className="btn" to="/login">Log in</Link>
          <Link className="btn secondary" to="/register">Create account</Link>
        </div>
      )}
    </main>
  );
}
