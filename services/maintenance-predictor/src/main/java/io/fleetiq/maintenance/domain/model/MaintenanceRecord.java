package io.fleetiq.maintenance.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A maintenance event that actually occurred, distinct from a generated prediction.
 * Its metadata and telemetry snapshot can later provide tenant-scoped retrieval evidence.
 */
public record MaintenanceRecord(
    UUID eventId,
    String vin,
    String component,
    String description,
    Severity severity,
    Instant occurredAt,
    Instant recordedAt,
    Map<String, String> metadata,
    Map<String, Object> telemetrySnapshot
) {}
