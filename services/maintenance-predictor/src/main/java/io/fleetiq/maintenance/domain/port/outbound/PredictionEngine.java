package io.fleetiq.maintenance.domain.port.outbound;

import io.fleetiq.maintenance.domain.model.AnomalyAssessment;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.smallrye.mutiny.Uni;

/** Converts authorized, deterministic evidence into a prediction result. */
public interface PredictionEngine {
    Uni<PredictionResult> generate(String tenantId, String vin, AnomalyAssessment assessment);
}
