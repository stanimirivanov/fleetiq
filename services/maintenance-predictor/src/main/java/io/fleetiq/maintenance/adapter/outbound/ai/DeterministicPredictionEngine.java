package io.fleetiq.maintenance.adapter.outbound.ai;

import io.fleetiq.maintenance.domain.model.AnomalyAssessment;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.fleetiq.maintenance.domain.port.outbound.PredictionEngine;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/** Baseline prediction engine used before optional, evidence-constrained RAG enrichment. */
@ApplicationScoped
public class DeterministicPredictionEngine implements PredictionEngine {

    @Override
    public Uni<PredictionResult> generate(String tenantId, String vin, AnomalyAssessment assessment) {
        return Uni.createFrom().item(new PredictionResult(
            UUID.randomUUID(),
            vin,
            assessment.failureProbability(),
            assessment.component(),
            assessment.severity(),
            assessment.estimatedDaysUntilFailure(),
            assessment.recommendation(),
            assessment.evidenceIds()
        ));
    }
}
