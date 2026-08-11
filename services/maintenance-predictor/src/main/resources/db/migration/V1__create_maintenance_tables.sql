-- V1: Create maintenance record tables with pgvector support

-- Verify vector extension
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_extension WHERE extname = 'vector'
    ) THEN
        RAISE EXCEPTION 'pgvector extension is not installed';
END IF;
END $$;

-- Maintenance events table (JSONB document store pattern)
CREATE TABLE IF NOT EXISTS maintenance_events (
                                                  event_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vin             VARCHAR(17) NOT NULL,
    component       VARCHAR(100),
    description     TEXT,
    severity        VARCHAR(20) NOT NULL DEFAULT 'INFO',
    occurred_at     TIMESTAMPTZ NOT NULL,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata        JSONB DEFAULT '{}'::jsonb,
    telemetry_snapshot JSONB DEFAULT '{}'::jsonb
    );

CREATE INDEX IF NOT EXISTS idx_maintenance_vin ON maintenance_events (vin);
CREATE INDEX IF NOT EXISTS idx_maintenance_vin_time ON maintenance_events (vin, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_maintenance_severity ON maintenance_events (severity);
-- GIN index for JSONB queries
CREATE INDEX IF NOT EXISTS idx_maintenance_metadata_gin ON maintenance_events USING GIN (metadata);

-- Predictions table
CREATE TABLE IF NOT EXISTS maintenance_predictions (
                                                       prediction_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vin                     VARCHAR(17) NOT NULL,
    failure_probability     DOUBLE PRECISION NOT NULL,
    predicted_component     VARCHAR(100),
    estimated_days_until_failure INTEGER,
    recommendation          TEXT,
    evidence_ids            TEXT[],
    generated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actual_outcome          JSONB,
    outcome_recorded_at     TIMESTAMPTZ
    );

CREATE INDEX IF NOT EXISTS idx_predictions_vin ON maintenance_predictions (vin);
CREATE INDEX IF NOT EXISTS idx_predictions_probability ON maintenance_predictions (failure_probability DESC);

-- Embeddings table for vector similarity search (pgvector)
CREATE TABLE IF NOT EXISTS telemetry_embeddings (
                                                    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vin             VARCHAR(17) NOT NULL,
    window_start    TIMESTAMPTZ NOT NULL,
    window_end      TIMESTAMPTZ NOT NULL,
    embedding       vector(1536),  -- 1536 dimensions for OpenAI ada-002 compatible models
    anomaly_score   DOUBLE PRECISION,
    metadata        JSONB DEFAULT '{}'::jsonb
    );

-- IVFFlat index for approximate nearest neighbor search
CREATE INDEX IF NOT EXISTS idx_embeddings_vector
    ON telemetry_embeddings
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX IF NOT EXISTS idx_embeddings_vin_time
    ON telemetry_embeddings (vin, window_start DESC);

COMMENT ON TABLE maintenance_events IS 'Vehicle maintenance records stored as JSONB documents';
COMMENT ON TABLE maintenance_predictions IS 'AI-generated maintenance predictions with outcomes';
COMMENT ON TABLE telemetry_embeddings IS 'Vector embeddings of telemetry windows for similarity search';
