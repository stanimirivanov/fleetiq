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
import java.util.stream.StreamSupport;

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

    private static final String UPSERT_RELATIONSHIP = """
        INSERT INTO topology_relationship_projection
            (source_vin, target_vin, relationship_type, properties)
        VALUES ($1, $2, $3, $4::jsonb)
        ON CONFLICT (source_vin, target_vin, relationship_type)
        DO UPDATE SET properties = EXCLUDED.properties
        """;

    private static final String CONNECTED_NODES = """
        WITH RECURSIVE connected(vin, depth, path) AS (
            SELECT $1::varchar, 0, ARRAY[$1::varchar]
            UNION ALL
            SELECT CASE WHEN relationship.source_vin = connected.vin
                        THEN relationship.target_vin ELSE relationship.source_vin END,
                   connected.depth + 1,
                   connected.path || CASE WHEN relationship.source_vin = connected.vin
                                          THEN relationship.target_vin ELSE relationship.source_vin END
            FROM connected
            JOIN topology_relationship_projection relationship
              ON relationship.source_vin = connected.vin OR relationship.target_vin = connected.vin
            WHERE connected.depth < $2
              AND NOT (CASE WHEN relationship.source_vin = connected.vin
                            THEN relationship.target_vin ELSE relationship.source_vin END = ANY(connected.path))
        )
        SELECT projection.vin, projection.device_type, projection.status, min(connected.depth) AS depth
        FROM connected
        JOIN topology_vehicle_projection projection ON projection.vin = connected.vin
        GROUP BY projection.vin, projection.device_type, projection.status
        ORDER BY depth, projection.vin
        """;

    private static final String NEARBY_VEHICLES = """
        SELECT vin,
            6371 * 2 * asin(sqrt(
                power(sin(radians(latitude - $1) / 2), 2) +
                cos(radians($1)) * cos(radians(latitude)) *
                power(sin(radians(longitude - $2) / 2), 2)
            )) AS distance_km
        FROM topology_vehicle_projection
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
          AND 6371 * 2 * asin(sqrt(
                power(sin(radians(latitude - $1) / 2), 2) +
                cos(radians($1)) * cos(radians(latitude)) *
                power(sin(radians(longitude - $2) / 2), 2)
              )) <= $3
        ORDER BY distance_km, vin
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
        java.util.Map<String, Object> propertyValues = new java.util.HashMap<>(edge.properties());
        String properties = new io.vertx.core.json.JsonObject(propertyValues).encode();
        return pgPool.withTransaction(connection -> connection.preparedQuery(UPSERT_RELATIONSHIP)
            .execute(Tuple.of(edge.sourceVin(), edge.targetVin(), edge.relationshipType(), properties))
            .call(() -> connection.preparedQuery(
                    "SELECT fleetiq_sync_relationship_edge($1, $2, $3, $4::jsonb)")
                .execute(Tuple.of(edge.sourceVin(), edge.targetVin(), edge.relationshipType(), properties))))
            .replaceWithVoid();
    }

    @Override
    public Uni<List<TopologyNode>> findConnectedNodes(String vin, int maxDepth) {
        if (maxDepth < 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("Maximum depth cannot be negative"));
        }
        return pgPool.preparedQuery(CONNECTED_NODES)
            .execute(Tuple.of(vin, maxDepth))
            .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                .map(row -> new TopologyNode(row.getString("vin"), row.getString("device_type"),
                    row.getString("status")))
                .toList());
    }

    @Override
    public Uni<List<String>> findNearby(double latitude, double longitude, double radiusKm) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180 || radiusKm < 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("Invalid proximity search parameters"));
        }
        return pgPool.preparedQuery(NEARBY_VEHICLES)
            .execute(Tuple.of(latitude, longitude, radiusKm))
            .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                .map(row -> row.getString("vin"))
                .toList());
    }
}
