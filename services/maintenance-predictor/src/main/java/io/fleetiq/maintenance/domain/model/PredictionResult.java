package io.fleetiq.maintenance.domain.model;

import java.util.List;
import java.util.UUID;

public record PredictionResult(
    UUID predictionId,
    String vin,
    double failureProbability,
    String predictedComponent,
    int estimatedDaysUntilFailure,
    String recommendation,
    List<String> evidenceIds
) {}
