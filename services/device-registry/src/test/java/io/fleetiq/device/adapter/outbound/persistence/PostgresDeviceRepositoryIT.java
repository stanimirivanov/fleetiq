package io.fleetiq.device.adapter.outbound.persistence;

import io.fleetiq.device.domain.port.outbound.DeviceRepository;
import io.fleetiq.device.domain.port.outbound.DeviceRepositoryContract;
import io.fleetiq.device.domain.model.Device;
import io.fleetiq.proto.events.v1.DeviceProjectionEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class PostgresDeviceRepositoryIT extends DeviceRepositoryContract {

    @Inject
    PostgresDeviceRepository repository;

    @Inject PgPool pgPool;

    @Override
    protected Uni<?> resetRepository() {
        return pgPool.query("TRUNCATE TABLE projection_outbox, devices").execute();
    }

    @Override
    protected DeviceRepository repository() {
        return repository;
    }

    @Test
    @RunOnVertxContext
    void savesDeviceAndOutboxEventAtomically(UniAsserter asserter) {
        asserter.execute(this::resetRepository);
        asserter.execute(() -> repository.save("tenant-a", Device.registerNew(
            "2HGFC2F59JH000001", "OBD", "FleetIQ", "Edge", 2025, Map.of(), 2026)));
        asserter.assertThat(() -> pgPool.query("SELECT payload FROM projection_outbox").execute(), rows -> {
            assertEquals(1, rows.size());
            try {
                var event = DeviceProjectionEvent.parseFrom(rows.iterator().next().getBuffer("payload").getBytes());
                assertEquals("2HGFC2F59JH000001", event.getVin());
                assertEquals("IDLE", event.getStatus());
                assertEquals("tenant-a", event.getTenantId());
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw new AssertionError(e);
            }
        });
    }
}
