package io.fleetiq.maintenance.domain.port.inbound;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Inbound boundary for producing maintenance predictions, recording observed
 * maintenance, and reading prediction history.
 */
public interface PredictMaintenanceUseCase {
    Uni<PredictionResult> predict(String vin, int lookbackDays);
    Uni<MaintenanceRecord> recordEvent(MaintenanceRecord record);
    Uni<List<PredictionResult>> getHistory(String vin, int limit);
}
