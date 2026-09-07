package io.fleetiq.maintenance.domain.model;

import java.util.List;

/** Advisory text and citations generated from an already-authorized evidence set. */
public record RecommendationAdvice(String recommendation, List<String> evidenceIds) {
    public RecommendationAdvice {
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }
}
