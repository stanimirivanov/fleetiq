ALTER TABLE topology_vehicle_projection ADD COLUMN tenant_id VARCHAR(100);
UPDATE topology_vehicle_projection SET tenant_id = 'legacy' WHERE tenant_id IS NULL;
ALTER TABLE topology_vehicle_projection ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE topology_relationship_projection ADD COLUMN tenant_id VARCHAR(100);
UPDATE topology_relationship_projection SET tenant_id = 'legacy' WHERE tenant_id IS NULL;
ALTER TABLE topology_relationship_projection ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE topology_relationship_projection
    DROP CONSTRAINT topology_relationship_projection_source_vin_fkey,
    DROP CONSTRAINT topology_relationship_projection_target_vin_fkey,
    DROP CONSTRAINT topology_relationship_projection_pkey;
ALTER TABLE topology_vehicle_projection DROP CONSTRAINT topology_vehicle_projection_pkey;
ALTER TABLE topology_vehicle_projection ADD PRIMARY KEY (tenant_id, vin);
ALTER TABLE topology_relationship_projection
    ADD PRIMARY KEY (tenant_id, source_vin, target_vin, relationship_type),
    ADD FOREIGN KEY (tenant_id, source_vin) REFERENCES topology_vehicle_projection(tenant_id, vin) ON DELETE CASCADE,
    ADD FOREIGN KEY (tenant_id, target_vin) REFERENCES topology_vehicle_projection(tenant_id, vin) ON DELETE CASCADE;

DROP INDEX IF EXISTS idx_topology_relationship_target;
CREATE INDEX idx_topology_relationship_tenant_target
    ON topology_relationship_projection (tenant_id, target_vin);
DROP INDEX IF EXISTS idx_topology_vehicle_position;
CREATE INDEX idx_topology_vehicle_tenant_position
    ON topology_vehicle_projection (tenant_id, latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

DROP FUNCTION fleetiq_sync_vehicle_vertex(TEXT);
CREATE FUNCTION fleetiq_sync_vehicle_vertex(p_tenant_id TEXT, p_vin TEXT) RETURNS VOID
LANGUAGE plpgsql AS $function$
DECLARE vehicle topology_vehicle_projection%ROWTYPE; statement TEXT;
BEGIN
    SELECT * INTO STRICT vehicle FROM topology_vehicle_projection
    WHERE tenant_id = p_tenant_id AND vin = p_vin;
    LOAD 'age'; PERFORM set_config('search_path', 'ag_catalog,public', true);
    statement := format($cypher$
        SELECT * FROM ag_catalog.cypher('fleet_topology', $$
            MERGE (v:Vehicle {tenant_id: %L, vin: %L})
            SET v.device_type = %L, v.status = %L, v.latitude = %s, v.longitude = %s,
                v.altitude = %s, v.device_updated_at = %s, v.position_observed_at = %s
        $$) AS (result ag_catalog.agtype)$cypher$,
        vehicle.tenant_id, vehicle.vin, COALESCE(vehicle.device_type, ''), COALESCE(vehicle.status, ''),
        COALESCE(vehicle.latitude::text, 'null'), COALESCE(vehicle.longitude::text, 'null'),
        COALESCE(vehicle.altitude::text, 'null'),
        COALESCE((EXTRACT(EPOCH FROM vehicle.device_updated_at) * 1000)::bigint::text, 'null'),
        COALESCE((EXTRACT(EPOCH FROM vehicle.position_observed_at) * 1000)::bigint::text, 'null'));
    EXECUTE statement;
END;$function$;

DROP FUNCTION fleetiq_sync_relationship_edge(TEXT, TEXT, TEXT, JSONB);
CREATE FUNCTION fleetiq_sync_relationship_edge(p_tenant_id TEXT, p_source_vin TEXT,
    p_target_vin TEXT, p_relationship_type TEXT, p_properties JSONB) RETURNS VOID
LANGUAGE plpgsql AS $function$
DECLARE statement TEXT;
BEGIN
    LOAD 'age'; PERFORM set_config('search_path', 'ag_catalog,public', true);
    statement := format($cypher$
        SELECT * FROM ag_catalog.cypher('fleet_topology', $$
            MATCH (source:Vehicle {tenant_id: %L, vin: %L}),
                  (target:Vehicle {tenant_id: %L, vin: %L})
            MERGE (source)-[:connected_to {relationship_type: %L}]->(target)
        $$) AS (result ag_catalog.agtype)$cypher$,
        p_tenant_id, p_source_vin, p_tenant_id, p_target_vin, p_relationship_type);
    EXECUTE statement;
END;$function$;
