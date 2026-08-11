package io.fleetiq.telemetry;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

/**
 * Shared test profile that starts a TimescaleDB container.
 * Can be reused across all service integration tests.
 */
public class TestProfiles {

    public static class TimescaleDbTestResource implements QuarkusTestResourceLifecycleManager {

        private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            "timescale/timescaledb:2.16.1-pg16"
        )
            .withDatabaseName("telemetry_db")
            .withUsername("fleetiq")
            .withPassword("fleetiq_test");

        @Override
        public Map<String, String> start() {
            POSTGRES.start();

            return Map.of(
                "quarkus.datasource.jdbc.url", POSTGRES.getJdbcUrl(),
                "quarkus.datasource.username", POSTGRES.getUsername(),
                "quarkus.datasource.password", POSTGRES.getPassword(),
                "quarkus.datasource.devservices.enabled", "false"
            );
        }

        @Override
        public void stop() {
            POSTGRES.stop();
        }
    }
}
