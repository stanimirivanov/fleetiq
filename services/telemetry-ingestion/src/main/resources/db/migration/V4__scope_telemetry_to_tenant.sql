SELECT remove_compression_policy('telemetry_samples', if_exists => TRUE);
SELECT decompress_chunk(chunk, true)
FROM show_chunks('telemetry_samples') AS chunk;
ALTER TABLE telemetry_samples SET (timescaledb.compress = false);

ALTER TABLE telemetry_samples ADD COLUMN tenant_id VARCHAR(100);
UPDATE telemetry_samples SET tenant_id = 'legacy' WHERE tenant_id IS NULL;
ALTER TABLE telemetry_samples ALTER COLUMN tenant_id SET NOT NULL;

DROP INDEX IF EXISTS idx_telemetry_vin_time;
DROP INDEX IF EXISTS idx_telemetry_vin_speed;
CREATE INDEX idx_telemetry_tenant_vin_time
    ON telemetry_samples (tenant_id, vin, time DESC);
CREATE INDEX idx_telemetry_tenant_vin_speed
    ON telemetry_samples (tenant_id, vin, time DESC, speed_kmh);

ALTER TABLE telemetry_samples SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'tenant_id,vin',
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('telemetry_samples', INTERVAL '7 days',
    if_not_exists => TRUE);

SELECT remove_continuous_aggregate_policy('telemetry_hourly_summary', if_exists => TRUE);
DROP MATERIALIZED VIEW IF EXISTS telemetry_hourly_summary;

CREATE MATERIALIZED VIEW telemetry_hourly_summary
    WITH (timescaledb.continuous) AS
SELECT time_bucket('1 hour', time) AS bucket,
       tenant_id,
       vin,
       AVG(speed_kmh) AS avg_speed_kmh,
       AVG(fuel_level_pct) AS avg_fuel_pct,
       AVG(engine_temp_celsius) AS avg_engine_temp,
       MAX(speed_kmh) AS max_speed_kmh,
       COUNT(*) AS sample_count
FROM telemetry_samples
GROUP BY bucket, tenant_id, vin
WITH NO DATA;

SELECT add_continuous_aggregate_policy('telemetry_hourly_summary',
    start_offset => INTERVAL '2 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);
