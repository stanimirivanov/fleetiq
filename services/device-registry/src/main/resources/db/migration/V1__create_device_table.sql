-- V1: Create device registry tables

CREATE TABLE IF NOT EXISTS devices (
                                       vin             VARCHAR(17) PRIMARY KEY,
    device_type     VARCHAR(50) NOT NULL,
    manufacturer    VARCHAR(100),
    model           VARCHAR(100),
    year            INTEGER,
    capabilities    JSONB DEFAULT '{}'::jsonb,
    status          VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    registered_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_devices_status ON devices (status);
CREATE INDEX IF NOT EXISTS idx_devices_type ON devices (device_type);

-- Trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_devices_updated_at
    BEFORE UPDATE ON devices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE devices IS 'Registered vehicles and their metadata';
