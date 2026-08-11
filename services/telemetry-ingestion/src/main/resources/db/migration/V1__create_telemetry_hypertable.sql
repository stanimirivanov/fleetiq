-- V1: Create telemetry hypertable with TimescaleDB

CREATE TABLE IF NOT EXISTS telemetry_samples (
     time        TIMESTAMPTZ NOT NULL,
     vin         VARCHAR(17) NOT NULL,
     latitude    DOUBLE PRECISION,
     longitude   DOUBLE PRECISION,
     altitude    DOUBLE PRECISION,
     speed_kmh   DOUBLE PRECISION,
     fuel_level_pct DOUBLE PRECISION,
     engine_temp_celsius DOUBLE PRECISION,
     battery_voltage DOUBLE PRECISION,
     custom_metrics JSONB DEFAULT '{}'::jsonb
);

-- Convert to hypertable
SELECT create_hypertable('telemetry_samples', 'time',
    chunk_time_interval => INTERVAL '1 day',
    if_not_exists => TRUE
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_telemetry_vin_time
    ON telemetry_samples (vin, time DESC);

CREATE INDEX IF NOT EXISTS idx_telemetry_vin_speed
    ON telemetry_samples (vin, time DESC, speed_kmh);

-- Enable compression
ALTER TABLE telemetry_samples SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'vin',
    timescaledb.compress_orderby = 'time DESC'
);

-- Add compression policy: compress chunks older than 7 days
SELECT add_compression_policy('telemetry_samples', INTERVAL '7 days',
    if_not_exists => TRUE
);
