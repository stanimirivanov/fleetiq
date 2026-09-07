#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE telemetry_db;
    CREATE DATABASE device_registry_db;
    CREATE DATABASE pekko_journal_db;
    CREATE DATABASE keycloak_db;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d telemetry_db <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS timescaledb;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d device_registry_db <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d pekko_journal_db <<-EOSQL
    CREATE TABLE IF NOT EXISTS public.event_journal (
        ordering BIGSERIAL,
        persistence_id VARCHAR(255) NOT NULL,
        sequence_number BIGINT NOT NULL,
        deleted BOOLEAN DEFAULT FALSE NOT NULL,
        writer VARCHAR(255) NOT NULL,
        write_timestamp BIGINT,
        adapter_manifest VARCHAR(255),
        event_ser_id INTEGER NOT NULL,
        event_ser_manifest VARCHAR(255) NOT NULL,
        event_payload BYTEA NOT NULL,
        meta_ser_id INTEGER,
        meta_ser_manifest VARCHAR(255),
        meta_payload BYTEA,
        PRIMARY KEY (persistence_id, sequence_number)
    );
    CREATE UNIQUE INDEX IF NOT EXISTS event_journal_ordering_idx
        ON public.event_journal(ordering);

    CREATE TABLE IF NOT EXISTS public.snapshot (
        persistence_id VARCHAR(255) NOT NULL,
        sequence_number BIGINT NOT NULL,
        created BIGINT NOT NULL,
        snapshot_ser_id INTEGER NOT NULL,
        snapshot_ser_manifest VARCHAR(255) NOT NULL,
        snapshot_payload BYTEA NOT NULL,
        meta_ser_id INTEGER,
        meta_ser_manifest VARCHAR(255),
        meta_payload BYTEA,
        PRIMARY KEY (persistence_id, sequence_number)
    );
EOSQL

echo "Core databases, TimescaleDB, and Pekko persistence tables created successfully"
