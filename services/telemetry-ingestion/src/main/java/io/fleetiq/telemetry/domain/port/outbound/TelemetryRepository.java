package io.fleetiq.telemetry.domain.port.outbound;

import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.List;

public interface TelemetryRepository {

    Uni<Void> save(TelemetrySample sample);

    Uni<List<TelemetrySample>> findByVinAndTimeRange(String vin, Instant from, Instant to);

    Uni<Double> getAverageSpeed(String vin, Instant from, Instant to);
}
