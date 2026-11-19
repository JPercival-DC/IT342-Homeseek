import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <main className="card narrow center">
      <h1>Page not found</h1>
      <Link className="btn" to="/">Go home</Link>
    </main>
  );
}
