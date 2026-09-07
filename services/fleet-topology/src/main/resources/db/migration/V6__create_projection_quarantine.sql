CREATE TABLE projection_quarantine (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payload BYTEA NOT NULL,
    failure_class VARCHAR(500) NOT NULL,
    failure_message VARCHAR(2000),
    attempts INTEGER NOT NULL CHECK (attempts > 0),
    quarantined_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_projection_quarantine_time
    ON projection_quarantine (quarantined_at DESC);
