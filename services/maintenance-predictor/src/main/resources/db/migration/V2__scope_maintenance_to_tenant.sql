ALTER TABLE maintenance_events ADD COLUMN tenant_id VARCHAR(100);
UPDATE maintenance_events SET tenant_id = 'legacy' WHERE tenant_id IS NULL;
ALTER TABLE maintenance_events ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE maintenance_predictions ADD COLUMN tenant_id VARCHAR(100);
UPDATE maintenance_predictions SET tenant_id = 'legacy' WHERE tenant_id IS NULL;
ALTER TABLE maintenance_predictions ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE telemetry_embeddings ADD COLUMN tenant_id VARCHAR(100);
UPDATE telemetry_embeddings SET tenant_id = 'legacy' WHERE tenant_id IS NULL;
ALTER TABLE telemetry_embeddings ALTER COLUMN tenant_id SET NOT NULL;

DROP INDEX IF EXISTS idx_maintenance_vin;
DROP INDEX IF EXISTS idx_maintenance_vin_time;
DROP INDEX IF EXISTS idx_predictions_vin;
DROP INDEX IF EXISTS idx_embeddings_vin_time;

CREATE INDEX idx_maintenance_tenant_vin ON maintenance_events (tenant_id, vin);
CREATE INDEX idx_maintenance_tenant_vin_time ON maintenance_events (tenant_id, vin, occurred_at DESC);
CREATE INDEX idx_predictions_tenant_vin ON maintenance_predictions (tenant_id, vin, generated_at DESC);
CREATE INDEX idx_embeddings_tenant_vin_time ON telemetry_embeddings (tenant_id, vin, window_start DESC);
