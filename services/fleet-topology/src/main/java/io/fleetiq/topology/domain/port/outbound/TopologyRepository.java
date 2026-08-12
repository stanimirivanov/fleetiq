package io.fleetiq.topology.domain.port.outbound;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.smallrye.mutiny.Uni;
import java.util.List;

public interface TopologyRepository {
    Uni<Void> createRelationship(TopologyEdge edge);
    Uni<List<TopologyNode>> findConnectedNodes(String vin, int maxDepth);
    Uni<List<String>> findNearby(double latitude, double longitude, double radiusKm);
}
