package io.fleetiq.maintenance.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Persistable semantic representation of one telemetry evidence window. */
public record TelemetryEmbedding(
    UUID id,
    String vin,
    Instant windowStart,
    Instant windowEnd,
    GeneratedEmbedding embedding,
    double anomalyScore,
    String content,
    UUID evidenceId,
    Map<String, Object> metadata
) {
    public TelemetryEmbedding {
        metadata = Map.copyOf(metadata);
    }
}
