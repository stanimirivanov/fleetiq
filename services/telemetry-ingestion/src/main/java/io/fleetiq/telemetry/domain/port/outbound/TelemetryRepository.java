package io.fleetiq.telemetry.domain.port.outbound;

import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.List;

/**
 * Reactive persistence boundary for telemetry samples and time-window aggregates.
 * Callers remain independent of TimescaleDB and its query model.
 */
public interface TelemetryRepository {

    /** Stores the sample and its position-projection outbox event atomically. */
    Uni<Void> save(String tenantId, TelemetrySample sample);

    /** Returns the bounded time window in reverse chronological order. */
    Uni<List<TelemetrySample>> findByVinAndTimeRange(String tenantId, String vin, Instant from, Instant to);

    /** Computes average speed without loading the underlying samples; empty windows return zero. */
    Uni<Double> getAverageSpeed(String tenantId, String vin, Instant from, Instant to);
}
