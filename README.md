# HomeSeek – Increment 1: User Accounts and Authentication

HomeSeek is a Smart Boarding House Finder Platform. This increment implements user registration, login, logout, and role-based access for Tenant, Owner, and Admin accounts.

**Technology Stack:** Spring Boot 3.5, Java 17+, React 18, Vite, PostgreSQL, Spring Security, JWT, and Flyway.

## Prerequisites
- JDK 17 or newer
- Maven 3.9+
- Node.js 18+ and npm
- PostgreSQL 16+ or Docker

## Setup

1. Create an environment file:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Update `.env` with your database credentials, JWT secret (at least 32 characters), and admin account details.

3. Start the database using Docker:

   ```bash
   docker compose up -d
   ```

   Alternatively, configure your local PostgreSQL or Supabase database in `.env`.

4. Database tables are automatically created by Flyway when the backend starts.

## Running the Application

Open two terminals from the project root.

**Backend:**
```bash
cd backend
mvn spring-boot:run
```
API: `http://localhost:8080`

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```
Application: `http://localhost:5173`

The initial Admin account is automatically created using the credentials configured in `.env`. Tenants and Owners can register through the application.

## API Endpoints

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/logout` | Authenticated |
| GET | `/api/auth/me` | Authenticated |
| GET | `/api/tenant/dashboard` | Tenant |
| GET | `/api/owner/dashboard` | Owner |
| GET | `/api/admin/dashboard` | Admin |
