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
                assertEquals(1, result.getInt(1));
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
