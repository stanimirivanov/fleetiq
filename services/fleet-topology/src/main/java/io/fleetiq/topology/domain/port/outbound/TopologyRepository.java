package io.fleetiq.topology.domain.port.outbound;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.fleetiq.topology.domain.model.VehicleProjection;
import io.smallrye.mutiny.Uni;
import java.util.List;

public interface TopologyRepository {
    Uni<Void> upsertVehicle(VehicleProjection vehicle);
    Uni<Void> updatePosition(String vin, double latitude, double longitude, double altitude,
                             java.time.Instant observedAt);
    Uni<Void> createRelationship(TopologyEdge edge);
    Uni<List<TopologyNode>> findConnectedNodes(String vin, int maxDepth);
    Uni<List<String>> findNearby(double latitude, double longitude, double radiusKm);
}
