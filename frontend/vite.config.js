import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the browser talks to Vite (5173) and /api is forwarded to the Spring Boot backend (8080).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': { target: process.env.VITE_BACKEND_URL || 'http://localhost:8080', changeOrigin: true } },
  },
});
