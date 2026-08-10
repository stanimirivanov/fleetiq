-- V1: Create telemetry hypertable with TimescaleDB

-- Verify extension exists (should be created by DB init)
-- Flyway will fail if extension is missing — intentional.

-- Telemetry samples table
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

-- Convert to hypertable (TimescaleDB)
-- Partition by time, 1-day chunks by default
SELECT create_hypertable('telemetry_samples', 'time',
                         chunk_time_interval => INTERVAL '1 day',
                         if_not_exists => TRUE
       );

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_telemetry_vin_time
    ON telemetry_samples (vin, time DESC);

CREATE INDEX IF NOT EXISTS idx_telemetry_vin_speed
    ON telemetry_samples (vin, time DESC, speed_kmh);

-- Enable compression (TimescaleDB feature)
ALTER TABLE telemetry_samples SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'vin',
    timescaledb.compress_orderby = 'time DESC'
    );

-- Add compression policy: compress chunks older than 7 days
SELECT add_compression_policy('telemetry_samples', INTERVAL '7 days',
                              if_not_exists => TRUE
       );

-- Create a continuous aggregate for hourly vehicle summaries
CREATE MATERIALIZED VIEW IF NOT EXISTS telemetry_hourly_summary
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    vin,
    AVG(speed_kmh) AS avg_speed_kmh,
    AVG(fuel_level_pct) AS avg_fuel_pct,
    AVG(engine_temp_celsius) AS avg_engine_temp,
    MAX(speed_kmh) AS max_speed_kmh,
    COUNT(*) AS sample_count
FROM telemetry_samples
GROUP BY bucket, vin;

-- Refresh policy for continuous aggregate
SELECT add_continuous_aggregate_policy('telemetry_hourly_summary',
                                       start_offset => INTERVAL '2 days',
                                       end_offset => INTERVAL '1 hour',
                                       schedule_interval => INTERVAL '1 hour',
                                       if_not_exists => TRUE
       );

COMMENT ON TABLE telemetry_samples IS 'Raw vehicle telemetry stored as TimescaleDB hypertable';
COMMENT ON MATERIALIZED VIEW telemetry_hourly_summary IS 'Hourly aggregated telemetry per vehicle';
