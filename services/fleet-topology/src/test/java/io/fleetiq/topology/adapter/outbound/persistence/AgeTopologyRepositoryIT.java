package io.fleetiq.topology.adapter.outbound.persistence;

import io.fleetiq.topology.domain.model.VehicleProjection;
import io.fleetiq.topology.domain.model.TopologyEdge;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class AgeTopologyRepositoryIT {

    private static final String TENANT = "tenant-a";
    private static final String VIN = "1HGCM82633A004352";
    private static final Instant NEWER = Instant.parse("2026-08-13T10:00:00Z");
    private static final Instant OLDER = Instant.parse("2026-08-13T09:00:00Z");

    @Inject AgeTopologyRepository repository;
    @Inject PgPool pgPool;

    @Test
    @RunOnVertxContext
    void deviceProjectionIsIdempotentAndRejectsStaleUpdates(UniAsserter asserter) {
        asserter.execute(this::reset);
        asserter.execute(() -> repository.upsertVehicle(TENANT, vehicle("ACTIVE", NEWER)));
        asserter.execute(() -> repository.upsertVehicle(TENANT, vehicle("IDLE", OLDER)));
        asserter.execute(() -> repository.upsertVehicle(TENANT, vehicle("ACTIVE", NEWER)));
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
        asserter.execute(() -> repository.updatePosition(TENANT, VIN, 52.52, 13.405, 34, NEWER));
        asserter.execute(() -> repository.updatePosition(TENANT, VIN, 1, 2, 3, OLDER));
        asserter.assertThat(() -> pgPool.preparedQuery("""
            SELECT latitude, longitude FROM topology_vehicle_projection WHERE vin = $1
            """).execute(io.vertx.mutiny.sqlclient.Tuple.of(VIN)), rows -> {
            var row = rows.iterator().next();
            assertEquals(52.52, row.getDouble("latitude"));
            assertEquals(13.405, row.getDouble("longitude"));
        });
    }

    @Test
    @RunOnVertxContext
    void createsRelationshipIdempotentlyAndTraversesToBoundedDepth(UniAsserter asserter) {
        String second = "JH4TB2H26CC000001";
        String third = "2HGFC2F59JH000001";
        asserter.execute(this::reset);
        asserter.execute(() -> repository.upsertVehicle(TENANT, vehicle(VIN, "ACTIVE", NEWER)));
        asserter.execute(() -> repository.upsertVehicle(TENANT, vehicle(second, "IDLE", NEWER)));
        asserter.execute(() -> repository.upsertVehicle(TENANT, vehicle(third, "MAINTENANCE", NEWER)));
        asserter.execute(() -> repository.createRelationship(TENANT,
            new TopologyEdge(VIN, second, "CONVOY", Map.of("lane", "west"))));
        asserter.execute(() -> repository.createRelationship(TENANT,
            new TopologyEdge(VIN, second, "CONVOY", Map.of("lane", "east"))));
        asserter.execute(() -> repository.createRelationship(TENANT,
            new TopologyEdge(second, third, "TRAILER", Map.of())));

        asserter.assertThat(() -> repository.findConnectedNodes(TENANT, VIN, 1), nodes ->
            assertEquals(java.util.List.of(VIN, second), nodes.stream().map(node -> node.vin()).toList()));
        asserter.assertThat(() -> repository.findConnectedNodes(TENANT, VIN, 2), nodes ->
            assertEquals(java.util.List.of(VIN, second, third), nodes.stream().map(node -> node.vin()).toList()));
        asserter.assertThat(() -> pgPool.query("SELECT count(*) AS count FROM topology_relationship_projection")
            .execute(), rows -> assertEquals(2L, rows.iterator().next().getLong("count")));
    }

    @Test
    @RunOnVertxContext
    void findsNearbyVehiclesOrderedByDistance(UniAsserter asserter) {
        String nearby = "JH4TB2H26CC000001";
        String distant = "2HGFC2F59JH000001";
        asserter.execute(this::reset);
        asserter.execute(() -> repository.updatePosition(TENANT, VIN, 52.5200, 13.4050, 34, NEWER));
        asserter.execute(() -> repository.updatePosition(TENANT, nearby, 52.5210, 13.4050, 34, NEWER));
        asserter.execute(() -> repository.updatePosition(TENANT, distant, 53.0, 13.4050, 34, NEWER));
        asserter.assertThat(() -> repository.findNearby(TENANT, 52.5200, 13.4050, 1), vins ->
            assertEquals(java.util.List.of(VIN, nearby), vins));
    }

    @Test
    @RunOnVertxContext
    void isolatesProjectionQueriesAcrossTenants(UniAsserter asserter) {
        asserter.execute(this::reset);
        asserter.execute(() -> repository.updatePosition("tenant-a", VIN, 52.5200, 13.4050, 34, NEWER));
        asserter.execute(() -> repository.updatePosition("tenant-b", VIN, 40.7128, -74.0060, 10, NEWER));
        asserter.assertThat(() -> repository.findNearby("tenant-a", 52.5200, 13.4050, 1), vins ->
            assertEquals(java.util.List.of(VIN), vins));
        asserter.assertThat(() -> repository.findNearby("tenant-b", 52.5200, 13.4050, 1), vins ->
            assertEquals(java.util.List.of(), vins));
    }

    private io.smallrye.mutiny.Uni<?> reset() {
        return pgPool.query(
            "TRUNCATE TABLE topology_relationship_projection, topology_vehicle_projection")
            .execute();
    }

    private static VehicleProjection vehicle(String status, Instant updatedAt) {
        return vehicle(VIN, status, updatedAt);
    }

    private static VehicleProjection vehicle(String vin, String status, Instant updatedAt) {
        return new VehicleProjection(vin, "OBD", status, null, null, null, updatedAt, null);
    }
}
