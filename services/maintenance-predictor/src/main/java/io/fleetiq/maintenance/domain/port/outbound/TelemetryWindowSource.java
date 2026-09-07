package io.fleetiq.maintenance.domain.port.outbound;

import io.fleetiq.maintenance.domain.model.TelemetryWindow;
import io.smallrye.mutiny.Uni;

import java.time.Instant;

/** Retrieves tenant-scoped telemetry without granting maintenance access to telemetry storage. */
public interface TelemetryWindowSource {
    /** Loads readings for the inclusive time window through an authenticated service boundary. */
    Uni<TelemetryWindow> load(String tenantId, String vin, Instant from, Instant to);
}
