package io.fleetiq.device;

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
    void appliesDeviceRegistryMigration() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success")) {
                assertTrue(result.next());
                assertEquals(3, result.getInt(1));
            }
            try (var result = statement.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'tenant_id' AND is_nullable = 'NO'")) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
            try (var result = statement.executeQuery("SELECT to_regclass('public.projection_outbox') IS NOT NULL")) {
                assertTrue(result.next());
                assertTrue(result.getBoolean(1));
            }
            try (var result = statement.executeQuery("SELECT to_regclass('public.devices') IS NOT NULL")) {
                assertTrue(result.next());
                assertTrue(result.getBoolean(1));
            }
        }
    }
}
