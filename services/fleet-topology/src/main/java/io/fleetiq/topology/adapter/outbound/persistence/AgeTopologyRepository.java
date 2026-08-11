package io.fleetiq.topology.adapter.outbound.persistence;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.fleetiq.topology.domain.port.outbound.TopologyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class AgeTopologyRepository implements TopologyRepository {

    private static final Logger log = LoggerFactory.getLogger(AgeTopologyRepository.class);
    private final DataSource dataSource;

    public AgeTopologyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createRelationship(TopologyEdge edge) {
        log.debug("Creating AGE relationship: {} -[{}]-> {}",
            edge.sourceVin(), edge.relationshipType(), edge.targetVin());
    }

    @Override
    public List<TopologyNode> findConnectedNodes(String vin, int maxDepth) {
        log.debug("Querying AGE graph from VIN: {}, depth: {}", vin, maxDepth);
        return Collections.emptyList();
    }

    @Override
    public List<String> findNearby(double latitude, double longitude, double radiusKm) {
        log.debug("PostGIS nearby query: ({}, {}), radius: {}km", latitude, longitude, radiusKm);
        return Collections.emptyList();
    }
}
