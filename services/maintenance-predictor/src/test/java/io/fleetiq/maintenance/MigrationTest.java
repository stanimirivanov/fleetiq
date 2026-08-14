package io.fleetiq.maintenance;

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
    void appliesMaintenanceMigration() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success")) {
                assertTrue(result.next());
                assertEquals(3, result.getInt(1));
            }
            try (var result = statement.executeQuery("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'maintenance_predictions' AND column_name = 'severity'")) {
                assertTrue(result.next());
                assertEquals("NO", result.getString(1));
            }
            try (var result = statement.executeQuery("SELECT count(*) FROM information_schema.columns WHERE column_name = 'tenant_id' AND table_name IN ('maintenance_events', 'maintenance_predictions', 'telemetry_embeddings') AND is_nullable = 'NO'")) {
                assertTrue(result.next());
                assertEquals(3, result.getInt(1));
            }
            for (String table : new String[] {"maintenance_events", "maintenance_predictions", "telemetry_embeddings"}) {
                try (var result = statement.executeQuery("SELECT to_regclass('public." + table + "') IS NOT NULL")) {
                    assertTrue(result.next());
                    assertTrue(result.getBoolean(1), table + " should exist");
                }
            }
        }
    }
}
