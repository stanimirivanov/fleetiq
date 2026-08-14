package io.fleetiq.maintenance.domain.service;

import io.fleetiq.maintenance.domain.model.AnomalyAssessment;
import io.fleetiq.maintenance.domain.model.Severity;
import io.fleetiq.maintenance.domain.model.TelemetryReading;
import io.fleetiq.maintenance.domain.model.TelemetryWindow;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;

/**
 * Scores engine-temperature and battery-voltage anomalies with fixed, explainable
 * thresholds. The same window always produces the same assessment.
 */
@ApplicationScoped
public class TelemetryAnomalyDetector {

    public AnomalyAssessment assess(TelemetryWindow window) {
        if (window.readings().isEmpty()) {
            throw new IllegalArgumentException("Telemetry window contains no readings");
        }

        double maximumTemperature = window.readings().stream()
            .mapToDouble(TelemetryReading::engineTemperatureCelsius).max().orElseThrow();
        double minimumVoltage = window.readings().stream()
            .mapToDouble(TelemetryReading::batteryVoltage).min().orElseThrow();
        double temperatureThresholdScore = clamp((maximumTemperature - 90.0) / 30.0);
        double voltageThresholdScore = clamp((12.2 - minimumVoltage) / 2.2);
        double temperatureDeviationScore = upperDeviationScore(window.readings().stream()
            .mapToDouble(TelemetryReading::engineTemperatureCelsius).toArray());
        double voltageDeviationScore = lowerDeviationScore(window.readings().stream()
            .mapToDouble(TelemetryReading::batteryVoltage).toArray());
        double temperatureScore = Math.max(temperatureThresholdScore, temperatureDeviationScore);
        double voltageScore = Math.max(voltageThresholdScore, voltageDeviationScore);

        boolean engineAnomaly = temperatureScore >= voltageScore;
        double probability = round(Math.max(temperatureScore, voltageScore));
        String component = engineAnomaly ? "engine-cooling" : "battery";
        String evidence = engineAnomaly
            ? "telemetry:max-engine-temperature-c=" + format(maximumTemperature)
            : "telemetry:min-battery-voltage=" + format(minimumVoltage);

        return new AnomalyAssessment(
            probability,
            component,
            severity(probability),
            estimatedDays(probability),
            recommendation(component, probability),
            List.of(evidence)
        );
    }

    private static Severity severity(double probability) {
        if (probability >= 0.85) return Severity.CRITICAL;
        if (probability >= 0.60) return Severity.HIGH;
        if (probability >= 0.30) return Severity.MEDIUM;
        return Severity.LOW;
    }

    private static int estimatedDays(double probability) {
        return probability >= 0.85 ? 1 : probability >= 0.60 ? 7 : probability >= 0.30 ? 30 : 90;
    }

    private static String recommendation(String component, double probability) {
        if (probability < 0.30) return "Continue routine monitoring";
        return component.equals("engine-cooling")
            ? "Inspect the cooling system and verify coolant circulation"
            : "Test the battery and charging system";
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double upperDeviationScore(double[] values) {
        return deviationScore(values, java.util.Arrays.stream(values).max().orElseThrow());
    }

    private static double lowerDeviationScore(double[] values) {
        return deviationScore(values, java.util.Arrays.stream(values).min().orElseThrow());
    }

    private static double deviationScore(double[] values, double extreme) {
        if (values.length < 3) return 0.0;
        double mean = java.util.Arrays.stream(values).average().orElseThrow();
        double variance = java.util.Arrays.stream(values)
            .map(value -> Math.pow(value - mean, 2)).average().orElse(0.0);
        double standardDeviation = Math.sqrt(variance);
        if (standardDeviation < 0.000_001) return 0.0;
        return clamp(Math.abs(extreme - mean) / standardDeviation / 3.0);
    }

    private static double round(double value) {
        return Math.round(value * 1_000.0) / 1_000.0;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
