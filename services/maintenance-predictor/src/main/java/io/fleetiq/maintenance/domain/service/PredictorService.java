package io.fleetiq.maintenance.domain.service;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.port.inbound.PredictMaintenanceUseCase;
import io.fleetiq.maintenance.domain.port.outbound.MaintenanceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ApplicationScoped
public class PredictorService implements PredictMaintenanceUseCase {

    private static final Logger log = LoggerFactory.getLogger(PredictorService.class);
    private final MaintenanceRepository repository;

    public PredictorService(MaintenanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public PredictionResult predict(String vin, int lookbackDays) {
        log.debug("Predicting maintenance for VIN: {}, lookback: {} days", vin, lookbackDays);
        // Placeholder — AI integration in Phase 4
        return new PredictionResult(
            "placeholder-id", vin, 0.0, "unknown",
            999, "AI prediction not yet implemented", List.of()
        );
    }

    @Override
    public void recordEvent(MaintenanceRecord record) {
        log.debug("Recording maintenance event: {}", record.eventId());
        repository.saveEvent(record);
    }

    @Override
    public List<PredictionResult> getHistory(String vin, int limit) {
        return repository.findPredictionsByVin(vin, limit);
    }
}
