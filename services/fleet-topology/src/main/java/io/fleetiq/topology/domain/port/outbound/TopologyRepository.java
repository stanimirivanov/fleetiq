package io.fleetiq.topology.domain.port.outbound;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.fleetiq.topology.domain.model.VehicleProjection;
import io.smallrye.mutiny.Uni;
import java.util.List;

/**
 * Persistence boundary for the eventually consistent fleet graph. Implementations
 * must keep projection updates ordered so an older event cannot overwrite newer state.
 */
public interface TopologyRepository {
    Uni<Void> upsertVehicle(String tenantId, VehicleProjection vehicle);
    /** Applies a position only when {@code observedAt} is newer than the stored projection. */
    Uni<Void> updatePosition(String tenantId, String vin, double latitude, double longitude, double altitude,
                             java.time.Instant observedAt);
    Uni<Void> createRelationship(String tenantId, TopologyEdge edge);
    Uni<List<TopologyNode>> findConnectedNodes(String tenantId, String vin, int maxDepth);
    Uni<List<String>> findNearby(String tenantId, double latitude, double longitude, double radiusKm);
}
