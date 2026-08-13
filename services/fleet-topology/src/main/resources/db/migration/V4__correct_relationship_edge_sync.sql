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
            MERGE (source)-[:connected_to {relationship_type: %L}]->(target)
        $$) AS (result ag_catalog.agtype)
        $cypher$,
        p_source_vin, p_target_vin, p_relationship_type);
    EXECUTE statement;
END;
$function$;
