-- V1: Initialize Apache AGE graph for fleet topology

-- Verify AGE extension
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_extension WHERE extname = 'age'
    ) THEN
        RAISE EXCEPTION 'Apache AGE extension is not installed';
END IF;
END $$;

-- Set search_path to include ag_catalog
SET search_path = ag_catalog, "$user", public;

-- Create the fleet topology graph
-- Using AGE's create_graph function
SELECT * FROM ag_catalog.create_graph('fleet_topology');

-- Create vertex label for vehicles
SELECT * FROM ag_catalog.create_vlabel('fleet_topology', 'Vehicle');

-- Create edge label for relationships
SELECT * FROM ag_catalog.create_elabel('fleet_topology', 'connected_to');

-- Verify graph creation (will be logged by Flyway)
-- SELECT * FROM ag_catalog.ag_graph WHERE name = 'fleet_topology';

COMMENT ON SCHEMA ag_catalog IS 'Apache AGE graph catalog schema';
