ALTER TABLE maintenance_predictions
    ADD COLUMN IF NOT EXISTS severity VARCHAR(20) NOT NULL DEFAULT 'LOW';

ALTER TABLE maintenance_predictions
    ADD CONSTRAINT chk_prediction_severity
    CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));
