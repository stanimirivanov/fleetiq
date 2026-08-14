package io.fleetiq.maintenance.domain.service;

import io.fleetiq.maintenance.domain.model.Severity;
import io.fleetiq.maintenance.domain.model.TelemetryReading;
import io.fleetiq.maintenance.domain.model.TelemetryWindow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TelemetryAnomalyDetectorTest {

    private final TelemetryAnomalyDetector detector = new TelemetryAnomalyDetector();

    @Test
    void identifiesCriticalEngineTemperatureDeterministically() {
        var window = window(List.of(
            reading(92.0, 12.6),
            reading(118.0, 12.5)
        ));

        var first = detector.assess(window);
        var second = detector.assess(window);

        assertEquals(first, second);
        assertEquals("engine-cooling", first.component());
        assertEquals(Severity.CRITICAL, first.severity());
        assertEquals(0.933, first.failureProbability());
        assertEquals(List.of("telemetry:max-engine-temperature-c=118.00"), first.evidenceIds());
    }

    @Test
    void identifiesBatteryVoltageAnomaly() {
        var result = detector.assess(window(List.of(reading(86.0, 10.66))));

        assertEquals("battery", result.component());
        assertEquals(Severity.HIGH, result.severity());
        assertEquals(0.7, result.failureProbability());
    }

    @Test
    void detectsDeviationFromTheWindowBaselineBelowTheAbsoluteThreshold() {
        var result = detector.assess(window(List.of(
            reading(70.0, 12.6),
            reading(70.0, 12.6),
            reading(70.0, 12.6),
            reading(89.0, 12.6)
        )));

        assertEquals("engine-cooling", result.component());
        assertEquals(Severity.MEDIUM, result.severity());
        assertEquals(0.577, result.failureProbability());
    }

    @Test
    void rejectsEmptyEvidenceWindow() {
        assertThrows(IllegalArgumentException.class, () -> detector.assess(window(List.of())));
    }

    private static TelemetryWindow window(List<TelemetryReading> readings) {
        Instant to = Instant.parse("2026-08-14T12:00:00Z");
        return new TelemetryWindow("WVWZZZ1JZXW000001", to.minusSeconds(3600), to, readings);
    }

    private static TelemetryReading reading(double temperature, double voltage) {
        return new TelemetryReading(Instant.parse("2026-08-14T11:30:00Z"), temperature, voltage, 80.0);
    }
}
