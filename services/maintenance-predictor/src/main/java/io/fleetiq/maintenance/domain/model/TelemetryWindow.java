package io.fleetiq.maintenance.domain.model;

import java.time.Instant;
import java.util.List;

/** Tenant-authorized telemetry observations used as evidence for one prediction. */
public record TelemetryWindow(
    String vin,
    Instant from,
    Instant to,
    List<TelemetryReading> readings
) {
    public TelemetryWindow {
        readings = List.copyOf(readings);
    }
}
