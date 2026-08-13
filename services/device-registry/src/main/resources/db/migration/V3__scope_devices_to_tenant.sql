ALTER TABLE devices
    ADD COLUMN tenant_id VARCHAR(100);

UPDATE devices SET tenant_id = 'legacy' WHERE tenant_id IS NULL;

ALTER TABLE devices
    ALTER COLUMN tenant_id SET NOT NULL,
    DROP CONSTRAINT devices_pkey,
    ADD COLUMN id UUID DEFAULT gen_random_uuid();

ALTER TABLE devices
    ALTER COLUMN id SET NOT NULL,
    ADD CONSTRAINT devices_pkey PRIMARY KEY (id),
    ADD CONSTRAINT uq_devices_tenant_vin UNIQUE (tenant_id, vin);

DROP INDEX IF EXISTS idx_devices_status;
DROP INDEX IF EXISTS idx_devices_type;
CREATE INDEX idx_devices_tenant_status ON devices (tenant_id, status);
CREATE INDEX idx_devices_tenant_type ON devices (tenant_id, device_type);
