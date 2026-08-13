package io.fleetiq.topology.domain.port.inbound;

import io.fleetiq.topology.domain.model.VehicleProjection;
import io.smallrye.mutiny.Uni;

import java.time.Instant;

public interface TopologyProjectionUseCase {
    Uni<Void> projectDevice(VehicleProjection vehicle);
    Uni<Void> projectPosition(String vin, double latitude, double longitude, double altitude, Instant observedAt);
}
