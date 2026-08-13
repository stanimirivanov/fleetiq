package io.fleetiq.device.adapter.outbound.persistence;

import io.fleetiq.device.domain.model.Device;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mapping;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface DeviceMapper {
    Device toDomain(DeviceEntity entity);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    DeviceEntity toEntity(Device domain);
}
