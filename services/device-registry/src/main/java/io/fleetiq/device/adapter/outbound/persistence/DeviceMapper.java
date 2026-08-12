package io.fleetiq.device.adapter.outbound.persistence;

import io.fleetiq.device.domain.model.Device;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface DeviceMapper {
    Device toDomain(DeviceEntity entity);
    DeviceEntity toEntity(Device domain);
}
