-- FleetIQ: Database initialization
-- Creates logical databases per service and enables required extensions.

-- ── Telemetry Database (TimescaleDB + pgvector) ────────────
CREATE DATABASE telemetry_db;
\c telemetry_db
CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS vector;
-- Verify
SELECT extname, extversion FROM pg_extension
WHERE extname IN ('timescaledb', 'vector');

-- ── Device Registry Database ───────────────────────────────
CREATE DATABASE device_registry_db;
\c device_registry_db
-- No special extensions needed. Uses standard PostgreSQL.
-- Optionally enable pgcrypto for UUID generation:
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Fleet Topology Database (Apache AGE + PostGIS) ──────────
CREATE DATABASE topology_db;
\c topology_db
CREATE EXTENSION IF NOT EXISTS age;
CREATE EXTENSION IF NOT EXISTS postgis;
-- Load AGE. Ensure the search_path includes ag_catalog.
-- AGE creates its own schema; verify with:
-- SELECT * FROM ag_catalog.ag_graph;

-- ── Maintenance Database (JSONB + pgvector) ─────────────────
CREATE DATABASE maintenance_db;
\c maintenance_db
CREATE EXTENSION IF NOT EXISTS vector;
-- JSONB is built-in. No extension required.
-- pgcrypto for UUIDs:
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Pekko Persistence Journal Database ──────────────────────
CREATE DATABASE pekko_journal_db;
\c pekko_journal_db
-- Tables created by Pekko Persistence JDBC plugin at startup.
-- No manual schema creation needed here.

-- ── Keycloak Database ───────────────────────────────────────
CREATE DATABASE keycloak_db;
\c keycloak_db
-- Keycloak creates its own schema on first startup.

-- ── List all databases (for verification) ──────────────────
SELECT datname FROM pg_database WHERE datname LIKE '%_db' OR datname = 'keycloak_db'
ORDER BY datname;
