# HomeSeek – Increment 1: User Accounts and Authentication

Registration, login/logout and role-based dashboards (Tenant, Owner, Admin) for HomeSeek.
Stack: Spring Boot 3 (Java 17+) · React 18 (Vite) · PostgreSQL 16.

## Prerequisites
- JDK 17 or newer, Maven 3.9+
- Node.js 18+ and npm
- PostgreSQL 16 (local install, or Docker for the included `docker-compose.yml`)

## Setup
1. **Environment file.** Copy and edit it (it is git-ignored, never commit it):
   ```bash
   cp .env.example .env
   openssl rand -base64 48        # paste the output as JWT_SECRET
   ```
   Set `DB_PASSWORD`, `JWT_SECRET` (32+ characters) and `ADMIN_PASSWORD` (8+ characters, letter + number).
2. **Database.** Pick one:
   - Docker: `docker compose up -d` (reads `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` from `.env`).
   - Local PostgreSQL: create the database and user that match `.env`:
     ```sql
     CREATE USER homeseek WITH PASSWORD 'your-password';
     CREATE DATABASE homeseek OWNER homeseek;
     ```
   Tables (`users`, `revoked_tokens`) are created automatically by Flyway on first start.

## Run
```bash
# Terminal 1 – API on http://localhost:8080
cd backend && mvn spring-boot:run

# Terminal 2 – UI on http://localhost:5173
cd frontend && npm install && npm run dev
```
Open http://localhost:5173. The dev server proxies `/api` to the backend.

The first Admin is created at startup from `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Admins cannot self-register. Tenants and Owners register on the Create account page.

## Demo flow
1. Register a Tenant and an Owner, then log in as each: each lands on its own dashboard (`/tenant`, `/owner`).
2. Log in as the Admin: `/admin` shows user statistics.
3. While logged in as a Tenant, open `/admin`: the UI shows 403, and `GET /api/admin/dashboard` returns 403.
4. Log out: the token is revoked server-side, so reusing it returns 401.

## API
| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register` | public (role `TENANT` or `OWNER`) |
| POST | `/api/auth/login` | public |
| POST | `/api/auth/logout` | authenticated |
| GET | `/api/auth/me` | authenticated |
| GET | `/api/tenant/dashboard` | TENANT |
| GET | `/api/owner/dashboard` | OWNER |
| GET | `/api/admin/dashboard` | ADMIN |

## Security notes
- Passwords: BCrypt (cost 12). Policy: 8–72 characters, at least one letter and one number.
- Sessions: stateless JWT (24 h) sent as a Bearer header; logout stores the token id in `revoked_tokens`. The role is always read from the database, not from the token.
- Login: generic "Invalid email or password" message; 5 failed attempts lock the account for 15 minutes (kept in memory, so it resets on restart and is per instance).
- The token lives in `sessionStorage`, so it ends when the tab closes. An HttpOnly cookie is the upgrade path.
- Not in this increment: Google sign-in (FR-005) and profile editing (FR-008). The `google_id` and `auth_provider` columns are already there.

## Tests
```bash
cd backend && mvn test
```
`AuthFlowTest` covers registration, duplicates, validation, login, lockout, logout revocation and the role access matrix. It uses an in-memory H2 database in PostgreSQL mode, so no setup is needed.
