package io.fleetiq.topology.domain.port.outbound;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import java.util.List;

public interface TopologyRepository {
    void createRelationship(TopologyEdge edge);
    List<TopologyNode> findConnectedNodes(String vin, int maxDepth);
    List<String> findNearby(double latitude, double longitude, double radiusKm);
}
