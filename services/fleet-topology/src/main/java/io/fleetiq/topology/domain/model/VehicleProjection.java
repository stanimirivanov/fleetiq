package io.fleetiq.topology.domain.model;

import java.time.Instant;

public record VehicleProjection(
    String vin,
    String deviceType,
    String status,
    Double latitude,
    Double longitude,
    Double altitude,
    Instant deviceUpdatedAt,
    Instant positionObservedAt
) {
    public VehicleProjection withPosition(double latitude, double longitude, double altitude, Instant observedAt) {
        return new VehicleProjection(vin, deviceType, status, latitude, longitude, altitude,
            deviceUpdatedAt, observedAt);
    }
}
