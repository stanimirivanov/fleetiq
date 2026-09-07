package io.fleetiq.maintenance.adapter.outbound.ai;

import io.fleetiq.maintenance.domain.model.AnomalyAssessment;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.fleetiq.maintenance.domain.model.RecommendationAdvice;
import io.fleetiq.maintenance.domain.model.SimilarIncident;
import io.fleetiq.maintenance.domain.port.outbound.PredictionEngine;
import io.fleetiq.maintenance.domain.port.outbound.RecommendationModel;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Enriches deterministic predictions with local-model advice while enforcing that
 * every returned citation belongs to the authorized prompt evidence.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class RagPredictionEngine implements PredictionEngine {

    private final RecommendationModel recommendationModel;

    @Override
    public Uni<PredictionResult> generate(String tenantId, String vin, AnomalyAssessment assessment,
                                          List<SimilarIncident> similarIncidents) {
        return recommendationModel.recommend(vin, assessment, similarIncidents)
            .map(advice -> toPrediction(vin, assessment, similarIncidents, advice));
    }

    private static PredictionResult toPrediction(String vin, AnomalyAssessment assessment,
                                                   List<SimilarIncident> similarIncidents,
                                                   RecommendationAdvice advice) {
        if (advice.recommendation() == null || advice.recommendation().isBlank()
            || advice.recommendation().length() > 2_000) {
            throw new IllegalArgumentException("Recommendation model returned invalid advisory text");
        }

        var allowed = new HashSet<>(assessment.evidenceIds());
        similarIncidents.stream().map(SimilarIncident::evidenceId).filter(Objects::nonNull)
            .map(UUID::toString).forEach(allowed::add);
        if (!allowed.containsAll(advice.evidenceIds())) {
            throw new IllegalArgumentException("Recommendation model cited unauthorized evidence");
        }

        var citations = new java.util.ArrayList<>(assessment.evidenceIds());
        citations.addAll(advice.evidenceIds());
        return new PredictionResult(
            UUID.randomUUID(), vin, assessment.failureProbability(), assessment.component(),
            assessment.severity(), assessment.estimatedDaysUntilFailure(),
            advice.recommendation(), citations.stream().distinct().toList());
    }
}
