package io.fleetiq.device.domain.model;

import java.time.Instant;
import java.util.Map;

public record Device(
    String vin,
    String deviceType,
    String manufacturer,
    String model,
    int year,
    Map<String, String> capabilities,
    String status,
    Instant registeredAt,
    Instant updatedAt
) {}
