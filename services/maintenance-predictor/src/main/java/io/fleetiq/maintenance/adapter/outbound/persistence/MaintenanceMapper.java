package io.fleetiq.maintenance.adapter.outbound.persistence;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface MaintenanceMapper {
    MaintenanceRecord toDomain(MaintenanceEventEntity entity);
    @Mapping(target = "tenantId", ignore = true)
    MaintenanceEventEntity toEntity(MaintenanceRecord domain);

    PredictionResult toDomain(PredictionEntity entity);
    @Mapping(target = "tenantId", ignore = true)
    PredictionEntity toEntity(PredictionResult domain);
}
