package io.fleetiq.topology.domain.model;

public record TopologyNode(
    String vin,
    String deviceType,
    String status
) {}
