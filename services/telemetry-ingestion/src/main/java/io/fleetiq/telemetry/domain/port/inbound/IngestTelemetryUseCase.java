package io.fleetiq.telemetry.domain.port.inbound;

import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.List;

/**
 * Inbound boundary for accepting telemetry and querying the time-series projection.
 * All operations are lazy, non-blocking Mutiny pipelines.
 */
public interface IngestTelemetryUseCase {

    record IngestResult(boolean accepted, String message) {}

    /** Persists a sample and reports domain acceptance without blocking the caller. */
    Uni<IngestResult> ingest(String tenantId, TelemetrySample sample);

    /** Returns at most 1,000 samples in reverse chronological order for the inclusive window. */
    Uni<List<TelemetrySample>> getTelemetryRange(String tenantId, String vin, Instant from, Instant to);

    /** Returns the average speed in the window, or zero when no samples exist. */
    Uni<Double> getAverageSpeed(String tenantId, String vin, Instant from, Instant to);
}
