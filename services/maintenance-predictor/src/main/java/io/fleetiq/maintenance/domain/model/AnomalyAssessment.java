package io.fleetiq.maintenance.domain.model;

import java.util.List;

/** Deterministic interpretation of a telemetry window before any generative-AI enrichment. */
public record AnomalyAssessment(
    double failureProbability,
    String component,
    Severity severity,
    int estimatedDaysUntilFailure,
    String recommendation,
    List<String> evidenceIds
) {
    public AnomalyAssessment {
        evidenceIds = List.copyOf(evidenceIds);
    }
}
