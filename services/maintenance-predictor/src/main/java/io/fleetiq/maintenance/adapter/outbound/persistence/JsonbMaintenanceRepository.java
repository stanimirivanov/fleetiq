package io.fleetiq.maintenance.adapter.outbound.persistence;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.fleetiq.maintenance.domain.port.outbound.MaintenanceRepository;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class JsonbMaintenanceRepository implements MaintenanceRepository {

    private final MaintenanceMapper mapper;

    @Override
    public Uni<MaintenanceRecord> saveEvent(MaintenanceRecord record) {
        MaintenanceEventEntity entity = mapper.toEntity(record);

        return Panache.withTransaction(entity::persist)
            .map(persisted ->
                mapper.toDomain((MaintenanceEventEntity) persisted)
            )
            .onFailure().invoke(e ->
                log.error("Failed to save maintenance event for VIN: {}", record.vin(), e)
            );
    }

    @Override
    public Uni<PredictionResult> savePrediction(PredictionResult prediction) {
        PredictionEntity entity = mapper.toEntity(prediction);

        return Panache.withTransaction(entity::persist)
            .map(persisted ->
                mapper.toDomain((PredictionEntity) persisted)
            )
            .onFailure().invoke(e ->
                log.error("Failed to save prediction for VIN: {}", prediction.vin(), e)
            );
    }

    @Override
    public Uni<List<MaintenanceRecord>> findEventsByVin(String vin) {
        return MaintenanceEventEntity.<MaintenanceEventEntity>find("vin", Sort.by("occurredAt").descending(), vin)
            .list()
            .map(entities ->
                entities.stream()
                .map(mapper::toDomain)
                .toList()
            );
    }

    @Override
    public Uni<List<PredictionResult>> findPredictionsByVin(String vin, int limit) {
        return PredictionEntity.<PredictionEntity>find("vin", Sort.by("generatedAt").descending(), vin)
            .page(0, limit)
            .list()
            .map(entities ->
                entities.stream()
                .map(mapper::toDomain)
                .toList()
            );
    }
}
