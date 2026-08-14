package io.fleetiq.maintenance.domain.model;

import java.time.Instant;

/** Measurements needed by maintenance analysis, independent of telemetry transport types. */
public record TelemetryReading(
    Instant observedAt,
    double engineTemperatureCelsius,
    double batteryVoltage,
    double speedKmh
) {}
