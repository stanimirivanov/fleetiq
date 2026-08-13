CREATE TABLE topology_vehicle_projection (
    vin VARCHAR(17) PRIMARY KEY,
    device_type VARCHAR(50),
    status VARCHAR(20),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    altitude DOUBLE PRECISION,
    device_updated_at TIMESTAMPTZ,
    position_observed_at TIMESTAMPTZ
);

CREATE INDEX idx_topology_vehicle_position
    ON topology_vehicle_projection (latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

CREATE OR REPLACE FUNCTION fleetiq_sync_vehicle_vertex(p_vin TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    vehicle topology_vehicle_projection%ROWTYPE;
    statement TEXT;
BEGIN
    SELECT * INTO STRICT vehicle
    FROM topology_vehicle_projection
    WHERE vin = p_vin;

    LOAD 'age';
    PERFORM set_config('search_path', 'ag_catalog,public', true);
    statement := format($cypher$
        SELECT * FROM ag_catalog.cypher('fleet_topology', $$
            MERGE (v:Vehicle {vin: %L})
            SET v.device_type = %L,
                v.status = %L,
                v.latitude = %s,
                v.longitude = %s,
                v.altitude = %s,
                v.device_updated_at = %s,
                v.position_observed_at = %s
        $$) AS (result ag_catalog.agtype)
        $cypher$,
        vehicle.vin,
        COALESCE(vehicle.device_type, ''),
        COALESCE(vehicle.status, ''),
        COALESCE(vehicle.latitude::text, 'null'),
        COALESCE(vehicle.longitude::text, 'null'),
        COALESCE(vehicle.altitude::text, 'null'),
        COALESCE((EXTRACT(EPOCH FROM vehicle.device_updated_at) * 1000)::bigint::text, 'null'),
        COALESCE((EXTRACT(EPOCH FROM vehicle.position_observed_at) * 1000)::bigint::text, 'null'));
    EXECUTE statement;
END;
$function$;
