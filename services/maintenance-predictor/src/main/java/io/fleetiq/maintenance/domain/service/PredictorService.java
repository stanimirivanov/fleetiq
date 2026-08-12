package io.fleetiq.maintenance.domain.service;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.fleetiq.maintenance.domain.port.inbound.PredictMaintenanceUseCase;
import io.fleetiq.maintenance.domain.port.outbound.MaintenanceRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class PredictorService implements PredictMaintenanceUseCase {

    private final MaintenanceRepository repository;

    @Override
    public Uni<PredictionResult> predict(String vin, int lookbackDays) {
        log.info("Predicting maintenance for VIN: {}, lookback: {} days", vin, lookbackDays);
        // AI integration in Phase 4 — placeholder for now
        return Uni.createFrom().item(() ->
            new PredictionResult(
                UUID.randomUUID(),
                vin,
                0.0,
                "unknown",
                999,
                "AI prediction not yet implemented",
                List.of()
            )
        );
    }

    @Override
    public Uni<MaintenanceRecord> recordEvent(MaintenanceRecord record) {
        log.info("Recording maintenance event for VIN: {}", record.vin());
        return repository.saveEvent(record);
    }

    @Override
    public Uni<List<PredictionResult>> getHistory(String vin, int limit) {
        return repository.findPredictionsByVin(vin, limit);
    }
}
