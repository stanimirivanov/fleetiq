package io.fleetiq.topology.domain.port.inbound;

import io.fleetiq.topology.domain.model.VehicleProjection;
import io.smallrye.mutiny.Uni;

import java.time.Instant;

/**
 * Inbound boundary used by event consumers to update the topology's local,
 * eventually consistent vehicle projection.
 */
public interface TopologyProjectionUseCase {
    /** Applies device metadata unless the local projection already contains a newer event. */
    Uni<Void> projectDevice(String tenantId, VehicleProjection vehicle);

    /** Applies a location observation unless a newer position has already been projected. */
    Uni<Void> projectPosition(String tenantId, String vin, double latitude, double longitude,
                              double altitude, Instant observedAt);
}
