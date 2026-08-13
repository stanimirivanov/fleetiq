package io.fleetiq.device.adapter.outbound.persistence;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.fleetiq.device.domain.port.outbound.DeviceRepository;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor
public class PostgresDeviceRepository implements DeviceRepository {

    private final DeviceMapper mapper;

    @Override
    public Uni<Optional<Device>> findByVin(String vin) {
        return Panache.withSession(() ->
            DeviceEntity.<DeviceEntity>findById(vin)
                .map(entity -> Optional.ofNullable(entity).map(mapper::toDomain))
        );
    }

    @Override
    public Uni<Device> save(Device device) {
        DeviceEntity entity = mapper.toEntity(device);

        return Panache.withTransaction(entity::persist)
            .map(DeviceEntity.class::cast)
            .map(mapper::toDomain);
    }

    @Override
    public Uni<Optional<Device>> updateStatus(String vin, DeviceStatus status) {
        return Panache.withTransaction(() ->
                DeviceEntity.<DeviceEntity>findById(vin)
                    .onItem().ifNotNull().invoke(entity -> {
                        entity.status = status.name();
                    })
            )
            .map(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

}
