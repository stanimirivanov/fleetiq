CREATE TABLE maintenance_recommendation_outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    prediction_id UUID NOT NULL UNIQUE
        REFERENCES maintenance_predictions(prediction_id) ON DELETE CASCADE,
    payload BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recommendation_outbox_created_at
    ON maintenance_recommendation_outbox (created_at);
