package io.fleetiq.telemetry.domain.port.outbound;

import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.smallrye.mutiny.Uni;

public interface PositionEventPublisher {
    Uni<Void> publish(TelemetrySample sample);
}
