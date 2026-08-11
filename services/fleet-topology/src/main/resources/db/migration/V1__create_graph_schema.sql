-- V1: Initialize Apache AGE graph for fleet topology

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_extension WHERE extname = 'age'
    ) THEN
        RAISE EXCEPTION 'Apache AGE extension is not installed';
END IF;
END $$;

SET search_path = ag_catalog, "$user", public;

SELECT * FROM ag_catalog.create_graph('fleet_topology');
SELECT * FROM ag_catalog.create_vlabel('fleet_topology', 'Vehicle');
SELECT * FROM ag_catalog.create_elabel('fleet_topology', 'connected_to');
