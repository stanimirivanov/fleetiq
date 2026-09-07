package io.fleetiq.topology.domain.port.inbound;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.smallrye.mutiny.Uni;
import java.util.List;

/** Application boundary for tenant-scoped fleet relationships and spatial graph queries. */
public interface TopologyUseCase {
    /** Creates or updates a directed relationship within the tenant's fleet graph. */
    Uni<Void> createRelationship(String tenantId, TopologyEdge edge);

    /** Returns nodes reachable from the root VIN up to the requested traversal depth. */
    Uni<List<TopologyNode>> getFleetGraph(String tenantId, String rootVin, int maxDepth);

    /** Returns tenant vehicles within the radius, ordered from nearest to farthest. */
    Uni<List<String>> findNearbyVehicles(String tenantId, double latitude, double longitude, double radiusKm);
}
