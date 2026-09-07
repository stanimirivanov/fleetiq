package io.fleetiq.maintenance.domain.port.outbound;

import io.fleetiq.maintenance.domain.model.AnomalyAssessment;
import io.fleetiq.maintenance.domain.model.RecommendationAdvice;
import io.fleetiq.maintenance.domain.model.SimilarIncident;
import io.smallrye.mutiny.Uni;

import java.util.List;

/** Generates advisory text from deterministic assessment data and authorized evidence. */
public interface RecommendationModel {
    /** Returns advice whose citations must be drawn only from the supplied evidence. */
    Uni<RecommendationAdvice> recommend(
        String vin,
        AnomalyAssessment assessment,
        List<SimilarIncident> similarIncidents);
}
