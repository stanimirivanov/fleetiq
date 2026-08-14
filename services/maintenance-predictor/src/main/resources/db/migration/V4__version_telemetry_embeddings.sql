-- Embeddings are a derived projection. The original 1536-dimensional column was
-- a placeholder for a hosted model, so existing development vectors are rebuilt.
TRUNCATE TABLE telemetry_embeddings;
DROP INDEX IF EXISTS idx_embeddings_vector;

ALTER TABLE telemetry_embeddings
    ALTER COLUMN embedding TYPE vector(384);

ALTER TABLE telemetry_embeddings
    ADD COLUMN model_name VARCHAR(200) NOT NULL DEFAULT 'sentence-transformers/all-MiniLM-L6-v2',
    ADD COLUMN model_version VARCHAR(100) NOT NULL DEFAULT 'langchain4j-1.18.1-beta28',
    ADD COLUMN dimensions INTEGER NOT NULL DEFAULT 384,
    ADD COLUMN content TEXT NOT NULL DEFAULT '',
    ADD COLUMN evidence_id UUID REFERENCES maintenance_events(event_id) ON DELETE SET NULL;

ALTER TABLE telemetry_embeddings
    ADD CONSTRAINT chk_embedding_dimensions CHECK (dimensions = 384);

CREATE INDEX idx_embeddings_vector
    ON telemetry_embeddings USING hnsw (embedding vector_cosine_ops);

CREATE INDEX idx_embeddings_tenant_vin_model
    ON telemetry_embeddings (tenant_id, vin, model_name, model_version);
