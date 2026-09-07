package io.fleetiq.maintenance.domain.port.outbound;

import io.fleetiq.maintenance.domain.model.AnomalyAssessment;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.fleetiq.maintenance.domain.model.SimilarIncident;
import io.smallrye.mutiny.Uni;

/** Converts authorized, deterministic evidence into a prediction result. */
public interface PredictionEngine {
    /** Enriches the assessment without changing its component, probability, severity, or horizon. */
    Uni<PredictionResult> generate(String tenantId, String vin, AnomalyAssessment assessment,
                                   java.util.List<SimilarIncident> similarIncidents);
}
