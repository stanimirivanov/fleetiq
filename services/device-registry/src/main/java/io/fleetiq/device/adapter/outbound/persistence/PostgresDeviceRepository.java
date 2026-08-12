package io.fleetiq.device.adapter.outbound.persistence;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.port.outbound.DeviceRepository;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class PostgresDeviceRepository implements DeviceRepository {

    private final DeviceMapper mapper;

    @Override
    public Uni<Optional<Device>> findByVin(String vin) {
        return DeviceEntity.<DeviceEntity>findById(vin)
            .map(entity ->
                Optional.ofNullable(entity).map(mapper::toDomain)
            );
    }

    @Override
    public Uni<Device> save(Device device) {
        DeviceEntity entity = mapper.toEntity(device);

        return Panache.withTransaction(entity::persist)
            .map(persisted ->
                mapper.toDomain((DeviceEntity) persisted)
            )
            .onFailure().invoke(e ->
                log.error("Failed to save device: {}", device.vin(), e)
            );
    }

    @Override
    public Uni<Device> updateStatus(String vin, String status) {
        return Panache.withTransaction(() ->
                DeviceEntity.<DeviceEntity>findById(vin)
                    .onItem().ifNotNull().invoke(entity -> {
                        // Dirty checking - updates when transaction completes
                        entity.status = status;
                    })
            )
            .onItem().ifNotNull().transform(mapper::toDomain)
            .onItem().ifNull().failWith(() ->
                new IllegalArgumentException("Device not found: " + vin)
            );
    }

}
