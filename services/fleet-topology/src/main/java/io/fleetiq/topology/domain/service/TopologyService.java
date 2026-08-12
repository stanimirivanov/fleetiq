package io.fleetiq.topology.domain.service;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.fleetiq.topology.domain.port.inbound.TopologyUseCase;
import io.fleetiq.topology.domain.port.outbound.TopologyRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class TopologyService implements TopologyUseCase {

    private final TopologyRepository repository;

    public TopologyService(TopologyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Uni<Void> createRelationship(TopologyEdge edge) {
        return repository.createRelationship(edge);
    }

    @Override
    public Uni<List<TopologyNode>> getFleetGraph(String rootVin, int maxDepth) {
        return repository.findConnectedNodes(rootVin, maxDepth);
    }

    @Override
    public Uni<List<String>> findNearbyVehicles(double latitude, double longitude, double radiusKm) {
        return repository.findNearby(latitude, longitude, radiusKm);
    }
}
