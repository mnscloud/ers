-- Creates the ERS application role and database on an existing PostgreSQL server.
-- Works on Postgres 13+ (uses \gexec, supported by any modern psql client).
--
-- Run as a superuser:
--   Windows : & 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -f deploy\db\init-db.sql
--   Ubuntu  : sudo -u postgres psql -f deploy/db/init-db.sql
--
-- CHANGE THE PASSWORD BELOW before using this anywhere but a throwaway local dev box,
-- then set the matching DB_PASSWORD in your .env / systemd EnvironmentFile.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ers') THEN
        CREATE ROLE ers LOGIN PASSWORD 'ers';
    END IF;
END
$$;

SELECT 'CREATE DATABASE ers OWNER ers ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ers')\gexec
