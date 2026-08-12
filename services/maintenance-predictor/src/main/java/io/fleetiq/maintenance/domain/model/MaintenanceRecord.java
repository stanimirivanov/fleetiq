package io.fleetiq.maintenance.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
