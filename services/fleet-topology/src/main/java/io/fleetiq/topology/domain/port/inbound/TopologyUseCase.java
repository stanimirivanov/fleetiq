package io.fleetiq.topology.domain.port.inbound;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.smallrye.mutiny.Uni;
import java.util.List;

public interface TopologyUseCase {
    Uni<Void> createRelationship(TopologyEdge edge);
    Uni<List<TopologyNode>> getFleetGraph(String rootVin, int maxDepth);
    Uni<List<String>> findNearbyVehicles(double latitude, double longitude, double radiusKm);
}
