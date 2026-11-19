import axios from 'axios';

const TOKEN_KEY = 'homeseek.token';

// Same-origin in development (Vite proxy). For a deployed build, set VITE_API_URL to the backend's address.
const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '' });

// ponytail: the token is kept in sessionStorage (gone when the tab closes, readable by scripts on this page).
// Upgrade path: an HttpOnly, SameSite cookie set by the backend, plus CSRF protection.
export const tokenStore = {
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (t) => sessionStorage.setItem(TOKEN_KEY, t),
  clear: () => sessionStorage.removeItem(TOKEN_KEY),
};

api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

// An expired, revoked, or invalid token on a protected call ends the session. Login/register errors are left to the form.
api.interceptors.response.use(
  (res) => res,
  (error) => {
    const url = error.config?.url || '';
    if (error.response?.status === 401 && !url.startsWith('/api/auth/login') && !url.startsWith('/api/auth/register')) {
      onUnauthorized();
    }
    return Promise.reject(error);
  }
);

/** Turns a failed request into { message, fieldErrors } for display. */
export function readError(error) {
  const data = error.response?.data;
  if (!error.response) return { message: 'Cannot reach the server. Please check your connection and try again.', fieldErrors: {} };
  return { message: data?.message || 'Something went wrong. Please try again.', fieldErrors: data?.errors || {} };
}

export default api;
