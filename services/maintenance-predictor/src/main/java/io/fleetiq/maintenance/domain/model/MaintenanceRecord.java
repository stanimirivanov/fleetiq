package io.fleetiq.maintenance.domain.model;

import java.time.Instant;
import java.util.Map;

public record MaintenanceRecord(
    String eventId,
    String vin,
    String component,
    String description,
    String severity,
    Instant occurredAt,
    Instant recordedAt,
    Map<String, String> metadata
) {}
