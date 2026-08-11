package io.fleetiq.topology.domain.port.inbound;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import java.util.List;

public interface TopologyUseCase {
    void createRelationship(TopologyEdge edge);
    List<TopologyNode> getFleetGraph(String rootVin, int maxDepth);
    List<String> findNearbyVehicles(double latitude, double longitude, double radiusKm);
}
