package io.fleetiq.telemetry.domain.port.outbound;

import io.fleetiq.telemetry.domain.model.TelemetrySample;

public interface TelemetryRepository {
    void save(TelemetrySample sample);
}
