package io.fleetiq.topology.adapter.outbound.persistence;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.fleetiq.topology.domain.port.outbound.TopologyRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AgeTopologyRepository implements TopologyRepository {

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
