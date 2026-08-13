CREATE TABLE topology_relationship_projection (
    source_vin VARCHAR(17) NOT NULL REFERENCES topology_vehicle_projection(vin) ON DELETE CASCADE,
    target_vin VARCHAR(17) NOT NULL REFERENCES topology_vehicle_projection(vin) ON DELETE CASCADE,
    relationship_type VARCHAR(50) NOT NULL,
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (source_vin, target_vin, relationship_type),
    CHECK (source_vin <> target_vin)
);

CREATE INDEX idx_topology_relationship_target
    ON topology_relationship_projection (target_vin);

CREATE OR REPLACE FUNCTION fleetiq_sync_relationship_edge(
    p_source_vin TEXT,
    p_target_vin TEXT,
    p_relationship_type TEXT,
    p_properties JSONB
)
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    statement TEXT;
BEGIN
    LOAD 'age';
    PERFORM set_config('search_path', 'ag_catalog,public', true);
    statement := format($cypher$
        SELECT * FROM ag_catalog.cypher('fleet_topology', $$
            MATCH (source:Vehicle {vin: %L}), (target:Vehicle {vin: %L})
            MERGE (source)-[edge:connected_to {relationship_type: %L}]->(target)
            SET edge.properties_json = %L
        $$) AS (result ag_catalog.agtype)
        $cypher$,
        p_source_vin, p_target_vin, p_relationship_type, p_properties::text);
    EXECUTE statement;
END;
$function$;
