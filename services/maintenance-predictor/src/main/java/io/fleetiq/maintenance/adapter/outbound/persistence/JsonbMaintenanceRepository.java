package io.fleetiq.maintenance.adapter.outbound.persistence;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.port.inbound.PredictMaintenanceUseCase.PredictionResult;
import io.fleetiq.maintenance.domain.port.outbound.MaintenanceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class JsonbMaintenanceRepository implements MaintenanceRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonbMaintenanceRepository.class);
    private final DataSource dataSource;

    public JsonbMaintenanceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void saveEvent(MaintenanceRecord record) {
        log.debug("Saving maintenance event as JSONB: {}", record.eventId());
    }

    @Override
    public void savePrediction(PredictionResult prediction) {
        log.debug("Saving prediction: {}", prediction.predictionId());
    }

    @Override
    public List<MaintenanceRecord> findEventsByVin(String vin) {
        return Collections.emptyList();
    }

    @Override
    public List<PredictionResult> findPredictionsByVin(String vin, int limit) {
        return Collections.emptyList();
    }

    @Override
    public List<MaintenanceRecord> findSimilarEvents(double[] embedding, int limit) {
        log.debug("pgvector similarity search, limit: {}", limit);
        return Collections.emptyList();
    }
}
