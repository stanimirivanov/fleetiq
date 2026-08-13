package io.fleetiq.device.adapter.outbound.persistence;

import io.fleetiq.device.domain.port.outbound.DeviceRepository;
import io.fleetiq.device.domain.port.outbound.DeviceRepositoryContract;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
class PostgresDeviceRepositoryIT extends DeviceRepositoryContract {

    @Inject
    PostgresDeviceRepository repository;

    @Override
    protected Uni<?> resetRepository() {
        return Panache.withTransaction(() -> DeviceEntity.deleteAll());
    }

    @Override
    protected DeviceRepository repository() {
        return repository;
    }
}
