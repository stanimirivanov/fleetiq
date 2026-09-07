package io.fleetiq.maintenance.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Advisory maintenance result grounded in deterministic telemetry analysis and cited evidence.
 * It is a decision-support artifact, not a command to perform maintenance automatically.
 */
public record PredictionResult(
    UUID predictionId,
    String vin,
    double failureProbability,
    String predictedComponent,
    Severity severity,
    int estimatedDaysUntilFailure,
    String recommendation,
    List<String> evidenceIds
) {}
