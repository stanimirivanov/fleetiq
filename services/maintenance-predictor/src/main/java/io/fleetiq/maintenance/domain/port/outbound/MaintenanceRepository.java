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
    Uni<MaintenanceRecord> saveEvent(String tenantId, MaintenanceRecord record);
    Uni<PredictionResult> savePrediction(String tenantId, PredictionResult prediction);
    Uni<List<MaintenanceRecord>> findEventsByVin(String tenantId, String vin);
    Uni<List<PredictionResult>> findPredictionsByVin(String tenantId, String vin, int limit);
}
