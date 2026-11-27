-- Increment 2: lets a password change or reset end every session issued before it.
-- Each login token carries the version it was issued under; the filter rejects a token whose version is older.
ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
