package io.fleetiq.topology.adapter.outbound.persistence;

import io.fleetiq.topology.domain.model.VehicleProjection;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class AgeTopologyRepositoryIT {

    private static final String VIN = "1HGCM82633A004352";
    private static final Instant NEWER = Instant.parse("2026-08-13T10:00:00Z");
    private static final Instant OLDER = Instant.parse("2026-08-13T09:00:00Z");

    @Inject AgeTopologyRepository repository;
    @Inject PgPool pgPool;

    @Test
    @RunOnVertxContext
    void deviceProjectionIsIdempotentAndRejectsStaleUpdates(UniAsserter asserter) {
        asserter.execute(this::reset);
        asserter.execute(() -> repository.upsertVehicle(vehicle("ACTIVE", NEWER)));
        asserter.execute(() -> repository.upsertVehicle(vehicle("IDLE", OLDER)));
        asserter.execute(() -> repository.upsertVehicle(vehicle("ACTIVE", NEWER)));
        asserter.assertThat(() -> pgPool.preparedQuery("""
            SELECT count(*) AS count, max(status) AS status
            FROM topology_vehicle_projection WHERE vin = $1
            """).execute(io.vertx.mutiny.sqlclient.Tuple.of(VIN)), rows -> {
            var row = rows.iterator().next();
            assertEquals(1L, row.getLong("count"));
            assertEquals("ACTIVE", row.getString("status"));
        });
    }

    @Test
    @RunOnVertxContext
    void positionProjectionIsIdempotentAndRejectsStaleUpdates(UniAsserter asserter) {
        asserter.execute(this::reset);
        asserter.execute(() -> repository.updatePosition(VIN, 52.52, 13.405, 34, NEWER));
        asserter.execute(() -> repository.updatePosition(VIN, 1, 2, 3, OLDER));
        asserter.assertThat(() -> pgPool.preparedQuery("""
            SELECT latitude, longitude FROM topology_vehicle_projection WHERE vin = $1
            """).execute(io.vertx.mutiny.sqlclient.Tuple.of(VIN)), rows -> {
            var row = rows.iterator().next();
            assertEquals(52.52, row.getDouble("latitude"));
            assertEquals(13.405, row.getDouble("longitude"));
        });
    }

    private io.smallrye.mutiny.Uni<?> reset() {
        return pgPool.query("TRUNCATE TABLE topology_vehicle_projection").execute();
    }

    private static VehicleProjection vehicle(String status, Instant updatedAt) {
        return new VehicleProjection(VIN, "OBD", status, null, null, null, updatedAt, null);
    }
}
