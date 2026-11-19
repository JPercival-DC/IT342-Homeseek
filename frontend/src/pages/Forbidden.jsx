import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';
import { ROLE_HOME } from '../roles.js';

export default function Forbidden() {
  const { user } = useAuth();
  return (
    <main className="card narrow center">
      <h1>403: Access denied</h1>
      <p className="muted">Your account does not have permission to view that page.</p>
      <Link className="btn" to={user ? ROLE_HOME[user.role] : '/login'}>{user ? 'Back to my dashboard' : 'Log in'}</Link>
    </main>
  );
}
