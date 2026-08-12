package io.fleetiq.telemetry.domain.port.inbound;

import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.List;

public interface IngestTelemetryUseCase {

    record IngestResult(boolean accepted, String message) {}

    Uni<IngestResult> ingest(TelemetrySample sample);

    Uni<List<TelemetrySample>> getTelemetryRange(String vin, Instant from, Instant to);

    Uni<Double> getAverageSpeed(String vin, Instant from, Instant to);
}
