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
    public Uni<MaintenanceRecord> saveEvent(String tenantId, MaintenanceRecord record) {
        MaintenanceEventEntity entity = mapper.toEntity(record);
        entity.tenantId = tenantId;

        return Panache.withTransaction(entity::persist)
            .map(persisted ->
                mapper.toDomain((MaintenanceEventEntity) persisted)
            );
    }

    @Override
    public Uni<PredictionResult> savePrediction(String tenantId, PredictionResult prediction) {
        PredictionEntity entity = mapper.toEntity(prediction);
        entity.tenantId = tenantId;

        return Panache.withTransaction(entity::persist)
            .map(persisted ->
                mapper.toDomain((PredictionEntity) persisted)
            );
    }

    @Override
    public Uni<List<MaintenanceRecord>> findEventsByVin(String tenantId, String vin) {
        return Panache.withSession(() -> MaintenanceEventEntity.<MaintenanceEventEntity>find(
                    "tenantId = ?1 and vin = ?2", Sort.by("occurredAt").descending(), tenantId, vin)
                .list()
                .map(entities -> entities.stream().map(mapper::toDomain).toList()));
    }

    @Override
    public Uni<List<PredictionResult>> findPredictionsByVin(String tenantId, String vin, int limit) {
        return Panache.withSession(() -> PredictionEntity.<PredictionEntity>find(
                    "tenantId = ?1 and vin = ?2", Sort.by("generatedAt").descending(), tenantId, vin)
                .page(0, limit)
                .list()
                .map(entities -> entities.stream().map(mapper::toDomain).toList()));
    }
}
