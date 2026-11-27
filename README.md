# HomeSeek – Increments 1 and 2

- **Increment 1:** registration, login/logout, role-based dashboards (Tenant, Owner, Admin).
- **Increment 2:** user profile (view/edit), forgot/reset password (Gmail SMTP), change password, Row Level Security.

Stack: Spring Boot 3 (Java 17+) · React 18 (Vite) · Supabase PostgreSQL · Gmail SMTP.

## Prerequisites
- JDK 17+, Maven 3.9+
- Node.js 18+ and npm
- A free Supabase project (https://supabase.com), used as the PostgreSQL database
- A Gmail account with 2-Step Verification, to send password reset emails

## Installation
```bash
cp .env.example .env                 # then fill it in (see below)
openssl rand -base64 48              # paste the output as JWT_SECRET
cd frontend && npm install
```

## Supabase configuration (database only)
1. **Create a project.** Remember the database password you set.
2. **Database connection.** Project Settings → Database → Connection string → *Session pooler* (port 5432). Put the host, user (`postgres.<ref>`) and password in `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Do not use the transaction pooler (port 6543): it breaks Flyway migrations.
3. **Start the backend once** (see Run). Flyway creates the tables (`users`, `revoked_tokens`, `password_reset_tokens`).
4. **Row Level Security.** Open the Supabase SQL Editor, paste `supabase/rls.sql` and run it. It is safe to run again.

Supabase Auth is **not** used. Only the Supabase PostgreSQL database is.

## Gmail SMTP configuration (password reset emails)
1. Turn on 2-Step Verification for the Gmail account (Google Account → Security).
2. Create an App Password: Google Account → Security → 2-Step Verification → App passwords. Name it "HomeSeek". Google shows 16 letters.
3. Put the Gmail address in `MAIL_USERNAME` and the 16 letters **without spaces** in `MAIL_APP_PASSWORD` in `.env`.
4. Never commit `.env`. Without these two values the app still runs, and "Forgot password" answers that recovery is not set up.

Gmail sends from the account you log in with, and has daily sending limits (about 500 emails for a normal account): fine for a class project.

## Run
```bash
# Terminal 1 – API on http://localhost:8080
cd backend && mvn spring-boot:run

# Terminal 2 – UI on http://localhost:5173
cd frontend && npm run dev
```
The first Admin is created from `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Admins cannot self-register.

Optional local database instead of Supabase: `docker compose up -d` and set `DB_URL=jdbc:postgresql://localhost:5432/homeseek`, `DB_USERNAME=homeseek`.

## Demo flow
1. Register a Tenant, log in, open **My Profile**, change the name and phone, save, reload: the change persists. Email and role are read-only.
2. Open **Change Password**: a wrong current password is rejected; a correct one replaces the password. Log out and confirm only the new password works.
3. Log out, click **Forgot password?**, enter the email, open the emailed link (valid 30 minutes), set a new password, log in with it. Open the same link again: it is rejected.
4. As a Tenant, `GET /api/admin/dashboard` returns 403, and `/api/profile` only ever returns the caller's own profile.

## API
| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | public |
| POST | `/api/auth/logout`, GET `/api/auth/me` | authenticated |
| GET/PUT | `/api/profile` | authenticated (own profile only; no id in the URL) |
| POST | `/api/password/change` | authenticated |
| POST | `/api/password/forgot`, `/api/password/reset` | public |
| GET | `/api/tenant/dashboard`, `/api/owner/dashboard`, `/api/admin/dashboard` | that role only |

## How password recovery works
1. `forgot` creates a random 256-bit token, stores only its SHA-256 hash in `password_reset_tokens`, and emails the link through Gmail SMTP. The answer is the same whether or not the email is registered, and the work runs in the background.
2. The link opens `/reset-password?token=…` (valid 30 minutes, SRS FR-010). The page removes the token from the address bar.
3. `reset` hashes the token, finds the row, marks it used in one atomic update (so it works once), then stores the new BCrypt hash in `users`. Any older link for the account is cancelled.
4. Changing or resetting a password raises the user's `token_version`, so every older login token stops working.
5. Asking again within 60 seconds sends no second email; a newer link replaces the older one.

## Security notes
- Passwords: BCrypt (cost 12); 8–72 characters with a letter and a number. Wrong "current password" guesses are limited to 5 per 15 minutes (in memory, resets on restart).
- RLS (`supabase/rls.sql`): the Supabase REST API cannot read password hashes or other users' rows; a signed-in Supabase user can edit only their own full name and phone. The Spring backend connects as the table owner, which RLS does not restrict, so its own checks (token + role + own-id-only) are what protect the API.
- `MAIL_APP_PASSWORD` and `DB_PASSWORD` are secrets: environment only, never committed. Reset tokens are stored hashed, so a database leak cannot be used to reset passwords.
- Not in scope yet: Google sign-in (FR-005), changing the login email.

## Tests
```bash
cd backend && mvn test
```
`AuthFlowTest` (Increment 1), `AccountFlowTest`, `PasswordResetMailerTest` and `PasswordResetCooldownTest` (Increment 2) use in-memory H2 and a mocked email sender, so no Supabase or Gmail account is needed to run them.
