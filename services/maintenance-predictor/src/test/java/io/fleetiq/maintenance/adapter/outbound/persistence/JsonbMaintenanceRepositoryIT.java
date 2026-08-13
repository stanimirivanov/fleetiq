package io.fleetiq.maintenance.adapter.outbound.persistence;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.model.Severity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class JsonbMaintenanceRepositoryIT {
    private static final String VIN = "1HGCM82633A004352";

    @Inject JsonbMaintenanceRepository repository;
    @Inject PgPool pgPool;

    @Test
    @RunOnVertxContext
    void isolatesIdenticalVehicleHistoryAcrossTenants(UniAsserter asserter) {
        asserter.execute(() -> pgPool.query(
            "TRUNCATE TABLE maintenance_events, maintenance_predictions, telemetry_embeddings").execute());
        asserter.execute(() -> repository.saveEvent("tenant-a", event("brakes")));
        asserter.execute(() -> repository.saveEvent("tenant-b", event("battery")));
        asserter.assertThat(() -> repository.findEventsByVin("tenant-a", VIN), events -> {
            assertEquals(1, events.size());
            assertEquals("brakes", events.getFirst().component());
        });
        asserter.assertThat(() -> repository.findEventsByVin("tenant-b", VIN), events -> {
            assertEquals(1, events.size());
            assertEquals("battery", events.getFirst().component());
        });
    }

    private static MaintenanceRecord event(String component) {
        return new MaintenanceRecord(UUID.randomUUID(), VIN, component, "inspection",
            Severity.LOW, Instant.parse("2026-08-13T10:00:00Z"), null, Map.of(), Map.of());
    }
}
