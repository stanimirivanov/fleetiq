package io.fleetiq.topology.adapter.outbound.persistence;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.fleetiq.topology.domain.model.VehicleProjection;
import io.fleetiq.topology.domain.port.outbound.TopologyRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class AgeTopologyRepository implements TopologyRepository {

    private final PgPool pgPool;

    private static final String UPSERT_DEVICE = """
        INSERT INTO topology_vehicle_projection
            (vin, device_type, status, device_updated_at)
        VALUES ($1, $2, $3, $4)
        ON CONFLICT (vin) DO UPDATE SET
            device_type = EXCLUDED.device_type,
            status = EXCLUDED.status,
            device_updated_at = EXCLUDED.device_updated_at
        WHERE topology_vehicle_projection.device_updated_at IS NULL
           OR topology_vehicle_projection.device_updated_at <= EXCLUDED.device_updated_at
        RETURNING vin
        """;

    private static final String UPSERT_POSITION = """
        INSERT INTO topology_vehicle_projection
            (vin, latitude, longitude, altitude, position_observed_at)
        VALUES ($1, $2, $3, $4, $5)
        ON CONFLICT (vin) DO UPDATE SET
            latitude = EXCLUDED.latitude,
            longitude = EXCLUDED.longitude,
            altitude = EXCLUDED.altitude,
            position_observed_at = EXCLUDED.position_observed_at
        WHERE topology_vehicle_projection.position_observed_at IS NULL
           OR topology_vehicle_projection.position_observed_at <= EXCLUDED.position_observed_at
        RETURNING vin
        """;

    @Override
    public Uni<Void> upsertVehicle(VehicleProjection vehicle) {
        return pgPool.withTransaction(connection -> connection.preparedQuery(UPSERT_DEVICE)
            .execute(Tuple.of(vehicle.vin(), vehicle.deviceType(), vehicle.status(),
                java.time.OffsetDateTime.ofInstant(vehicle.deviceUpdatedAt(), java.time.ZoneOffset.UTC)))
            .onItem().transformToUni(rows -> rows.size() == 0
                ? Uni.createFrom().voidItem()
                : connection.preparedQuery("SELECT fleetiq_sync_vehicle_vertex($1)")
                    .execute(Tuple.of(vehicle.vin())).replaceWithVoid()))
            .replaceWithVoid();
    }

    @Override
    public Uni<Void> updatePosition(String vin, double latitude, double longitude, double altitude,
                                    java.time.Instant observedAt) {
        return pgPool.withTransaction(connection -> connection.preparedQuery(UPSERT_POSITION)
            .execute(Tuple.of(vin, latitude, longitude, altitude,
                java.time.OffsetDateTime.ofInstant(observedAt, java.time.ZoneOffset.UTC)))
            .onItem().transformToUni(rows -> rows.size() == 0
                ? Uni.createFrom().voidItem()
                : connection.preparedQuery("SELECT fleetiq_sync_vehicle_vertex($1)")
                    .execute(Tuple.of(vin)).replaceWithVoid()))
            .replaceWithVoid();
    }

    @Override
    public Uni<Void> createRelationship(TopologyEdge edge) {
        // AGE persistence will be implemented against the reactive PostgreSQL client.
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<List<TopologyNode>> findConnectedNodes(String vin, int maxDepth) {
        return Uni.createFrom().item(List.of());
    }

    @Override
    public Uni<List<String>> findNearby(double latitude, double longitude, double radiusKm) {
        return Uni.createFrom().item(List.of());
    }
}
