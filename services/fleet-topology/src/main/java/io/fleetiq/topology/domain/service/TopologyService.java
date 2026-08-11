package io.fleetiq.topology.domain.service;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.fleetiq.topology.domain.port.inbound.TopologyUseCase;
import io.fleetiq.topology.domain.port.outbound.TopologyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ApplicationScoped
public class TopologyService implements TopologyUseCase {

    private static final Logger log = LoggerFactory.getLogger(TopologyService.class);
    private final TopologyRepository repository;

    public TopologyService(TopologyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void createRelationship(TopologyEdge edge) {
        log.debug("Creating relationship: {} -> {}", edge.sourceVin(), edge.targetVin());
        repository.createRelationship(edge);
    }

    @Override
    public List<TopologyNode> getFleetGraph(String rootVin, int maxDepth) {
        log.debug("Fetching fleet graph for root: {}, depth: {}", rootVin, maxDepth);
        return repository.findConnectedNodes(rootVin, maxDepth);
    }

    @Override
    public List<String> findNearbyVehicles(double latitude, double longitude, double radiusKm) {
        log.debug("Finding vehicles near: {}, {} within {}km", latitude, longitude, radiusKm);
        return repository.findNearby(latitude, longitude, radiusKm);
    }
}
