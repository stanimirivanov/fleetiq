package io.fleetiq.telemetry.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record TelemetrySample(
    String vin,
    Instant timestamp,
    double latitude,
    double longitude,
    double altitude,
    double speedKmh,
    double fuelLevelPct,
    double engineTempCelsius,
    double batteryVoltage,
    Map<String, Double> customMetrics
) {}
