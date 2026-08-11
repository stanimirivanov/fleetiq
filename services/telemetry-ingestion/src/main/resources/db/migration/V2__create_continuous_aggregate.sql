-- V2: Create continuous aggregate (must run outside transaction — Flyway handles this per-migration)

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
GROUP BY bucket, vin
WITH NO DATA;

-- Refresh policy for continuous aggregate
SELECT add_continuous_aggregate_policy('telemetry_hourly_summary',
    start_offset => INTERVAL '2 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE
);
