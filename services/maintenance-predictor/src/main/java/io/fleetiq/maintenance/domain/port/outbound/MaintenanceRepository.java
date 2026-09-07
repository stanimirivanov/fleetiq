package io.fleetiq.maintenance.domain.port.outbound;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Persistence boundary for maintenance evidence and generated predictions. It keeps
 * storage-specific JSON and entity representations outside the domain layer.
 */
public interface MaintenanceRepository {
    /** Stores an observed maintenance event inside the tenant boundary. */
    Uni<MaintenanceRecord> saveEvent(String tenantId, MaintenanceRecord record);
    /**
     * Atomically stores a prediction and, when requested, stages its recommendation
     * for asynchronous publication. This prevents a committed high-confidence
     * prediction from being lost between database persistence and broker delivery.
     */
    Uni<PredictionResult> savePrediction(String tenantId, PredictionResult prediction,
                                         boolean stageRecommendation);

    /** Returns observed events newest first for use as history and authorized evidence. */
    Uni<List<MaintenanceRecord>> findEventsByVin(String tenantId, String vin);

    /** Returns at most {@code limit} predictions newest first. */
    Uni<List<PredictionResult>> findPredictionsByVin(String tenantId, String vin, int limit);
}
