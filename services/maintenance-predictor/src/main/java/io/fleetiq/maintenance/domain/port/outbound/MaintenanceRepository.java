package io.fleetiq.maintenance.domain.port.outbound;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface MaintenanceRepository {
    Uni<MaintenanceRecord> saveEvent(MaintenanceRecord record);
    Uni<PredictionResult> savePrediction(PredictionResult prediction);
    Uni<List<MaintenanceRecord>> findEventsByVin(String vin);
    Uni<List<PredictionResult>> findPredictionsByVin(String vin, int limit);
}
