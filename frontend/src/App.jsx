import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './AuthContext.jsx';
import { ROLE_HOME } from './roles.js';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Home from './pages/Home.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Forbidden from './pages/Forbidden.jsx';
import NotFound from './pages/NotFound.jsx';
import ForgotPassword from './pages/ForgotPassword.jsx';
import ResetPassword from './pages/ResetPassword.jsx';
import Profile from './pages/Profile.jsx';
import ChangePassword from './pages/ChangePassword.jsx';

const ALL_ROLES = Object.keys(ROLE_HOME);

/** Pages for visitors only: a logged-in user is sent to their own dashboard instead. */
function GuestOnly({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <p className="center muted">Loading...</p>;
  return user ? <Navigate to={ROLE_HOME[user.role]} replace /> : children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<GuestOnly><Login /></GuestOnly>} />
      <Route path="/register" element={<GuestOnly><Register /></GuestOnly>} />
      <Route path="/tenant" element={<ProtectedRoute roles={['TENANT']}><Dashboard role="TENANT" /></ProtectedRoute>} />
      <Route path="/owner" element={<ProtectedRoute roles={['OWNER']}><Dashboard role="OWNER" /></ProtectedRoute>} />
      <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']}><Dashboard role="ADMIN" /></ProtectedRoute>} />
      <Route path="/forgot-password" element={<GuestOnly><ForgotPassword /></GuestOnly>} />
      {/* Not guest-only: a logged-in user who opens a reset link must still reach it. */}
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/profile" element={<ProtectedRoute roles={ALL_ROLES}><Profile /></ProtectedRoute>} />
      <Route path="/change-password" element={<ProtectedRoute roles={ALL_ROLES}><ChangePassword /></ProtectedRoute>} />
      <Route path="/forbidden" element={<Forbidden />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
