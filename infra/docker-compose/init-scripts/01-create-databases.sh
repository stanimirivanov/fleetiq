#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE telemetry_db;
    CREATE DATABASE device_registry_db;
    CREATE DATABASE topology_db;
    CREATE DATABASE maintenance_db;
    CREATE DATABASE pekko_journal_db;
    CREATE DATABASE keycloak_db;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d telemetry_db <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS timescaledb;
    CREATE EXTENSION IF NOT EXISTS vector;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d device_registry_db <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d topology_db <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS age;
    CREATE EXTENSION IF NOT EXISTS postgis;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d maintenance_db <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS vector;
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";
EOSQL

echo "All databases and extensions created successfully"
