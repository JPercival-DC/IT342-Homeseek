-- HomeSeek Increment 2: Row Level Security for profile data on Supabase.
-- Run once in the Supabase SQL Editor AFTER the backend has started for the first time (Flyway creates the tables).
-- Safe to run again.
--
-- Why: Supabase exposes every table in the "public" schema through its REST API (PostgREST) to the "anon" and
-- "authenticated" roles. HomeSeek's own backend connects as the table owner ("postgres"), which is not affected by
-- these rules, so the application keeps working. These rules only protect the data from the REST API:
--   * nobody can read password hashes, reset-token hashes or other users' rows,
--   * a signed-in Supabase user can read and edit only their own profile row, and only full_name and phone.

ALTER TABLE public.users          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.revoked_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.password_reset_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.flyway_schema_history ENABLE ROW LEVEL SECURITY;

-- Start from nothing, then grant only what the profile needs (column-level, so password_hash/role/is_active stay hidden).
REVOKE ALL ON public.users, public.revoked_tokens, public.password_reset_tokens, public.flyway_schema_history FROM anon, authenticated;
GRANT SELECT (user_id, email, full_name, phone, role, created_at) ON public.users TO authenticated;
GRANT UPDATE (full_name, phone)                                    ON public.users TO authenticated;

DROP POLICY IF EXISTS users_select_own ON public.users;
CREATE POLICY users_select_own ON public.users
    FOR SELECT TO authenticated
    USING (lower(email) = lower(auth.jwt() ->> 'email'));

DROP POLICY IF EXISTS users_update_own ON public.users;
CREATE POLICY users_update_own ON public.users
    FOR UPDATE TO authenticated
    USING      (lower(email) = lower(auth.jwt() ->> 'email'))
    WITH CHECK (lower(email) = lower(auth.jwt() ->> 'email'));

-- No policies on revoked_tokens, password_reset_tokens or flyway_schema_history: with RLS on, the REST API sees nothing there.
-- No INSERT or DELETE policy on users: accounts are created and removed only by the backend.
