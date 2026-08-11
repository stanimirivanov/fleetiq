package io.fleetiq.maintenance.domain.port.inbound;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import java.util.List;

public interface PredictMaintenanceUseCase {

    record PredictionResult(
        String predictionId,
        String vin,
        double failureProbability,
        String predictedComponent,
        int estimatedDaysUntilFailure,
        String recommendation,
        List<String> evidenceIds
    ) {}

    PredictionResult predict(String vin, int lookbackDays);
    void recordEvent(MaintenanceRecord record);
    List<PredictionResult> getHistory(String vin, int limit);
}
