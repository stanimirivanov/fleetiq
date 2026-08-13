package io.fleetiq.device.adapter.outbound.persistence;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.fleetiq.device.domain.port.outbound.DeviceRepository;
import io.fleetiq.proto.events.v1.DeviceProjectionEvent;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

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

        return Panache.withTransaction(() -> entity.persistAndFlush()
                .call(() -> persistOutbox(entity)))
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
                    .onItem().ifNotNull().call(entity -> entity.flush()
                        .call(() -> persistOutbox(entity)))
            )
            .map(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    private Uni<?> persistOutbox(DeviceEntity entity) {
        Instant occurredAt = java.util.Objects.requireNonNullElse(entity.updatedAt, entity.registeredAt);
        ProjectionOutboxEntity outbox = new ProjectionOutboxEntity();
        outbox.id = UUID.randomUUID();
        outbox.eventType = "device-projection.v1";
        outbox.payload = DeviceProjectionEvent.newBuilder()
            .setVin(entity.vin)
            .setDeviceType(entity.deviceType)
            .setStatus(entity.status)
            .setOccurredAtEpochMillis(occurredAt.toEpochMilli())
            .build().toByteArray();
        outbox.createdAt = occurredAt;
        return outbox.persist();
    }

}
