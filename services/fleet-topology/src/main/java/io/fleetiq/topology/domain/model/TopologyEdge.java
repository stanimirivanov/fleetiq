package io.fleetiq.topology.domain.model;

import java.util.Map;

public record TopologyEdge(
    String sourceVin,
    String targetVin,
    String relationshipType,
    Map<String, String> properties
) {}
