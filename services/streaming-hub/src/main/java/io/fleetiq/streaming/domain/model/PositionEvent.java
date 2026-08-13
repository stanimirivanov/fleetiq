package io.fleetiq.streaming.domain.model;

import java.time.Instant;

public record PositionEvent(
    String tenantId,
    String vin,
    Instant observedAt,
    double latitude,
    double longitude,
    double altitude,
    double speedKmh,
    String status
) {}
