package io.fleetiq.maintenance.domain.port.outbound;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.port.inbound.PredictMaintenanceUseCase.PredictionResult;
import java.util.List;

public interface MaintenanceRepository {
    void saveEvent(MaintenanceRecord record);
    void savePrediction(PredictionResult prediction);
    List<MaintenanceRecord> findEventsByVin(String vin);
    List<PredictionResult> findPredictionsByVin(String vin, int limit);
    List<MaintenanceRecord> findSimilarEvents(double[] embedding, int limit);
}
