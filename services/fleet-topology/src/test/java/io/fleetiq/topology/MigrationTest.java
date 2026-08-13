package io.fleetiq.topology;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class MigrationTest {
    @Inject DataSource dataSource;

    @Test
    void appliesTopologyMigration() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success")) {
                assertTrue(result.next());
                assertEquals(4, result.getInt(1));
            }
            try (var result = statement.executeQuery(
                "SELECT to_regclass('public.topology_relationship_projection') IS NOT NULL")) {
                assertTrue(result.next());
                assertTrue(result.getBoolean(1));
            }
            try (var result = statement.executeQuery(
                "SELECT to_regclass('public.topology_vehicle_projection') IS NOT NULL")) {
                assertTrue(result.next());
                assertTrue(result.getBoolean(1));
            }
            try (var result = statement.executeQuery("SELECT EXISTS (SELECT FROM ag_catalog.ag_graph WHERE name = 'fleet_topology')")) {
                assertTrue(result.next());
                assertTrue(result.getBoolean(1));
            }
        }
    }
}
