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
    /** Produces a grounded prediction from between 1 and 365 days of telemetry. */
    Uni<PredictionResult> predict(String tenantId, String vin, int lookbackDays);

    /** Records an observed maintenance event for later history and retrieval evidence. */
    Uni<MaintenanceRecord> recordEvent(String tenantId, MaintenanceRecord record);

    /** Returns the newest predictions for a tenant-scoped vehicle, bounded by {@code limit}. */
    Uni<List<PredictionResult>> getHistory(String tenantId, String vin, int limit);
}
