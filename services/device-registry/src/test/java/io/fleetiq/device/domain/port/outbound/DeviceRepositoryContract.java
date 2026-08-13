package io.fleetiq.device.domain.port.outbound;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class DeviceRepositoryContract {

    private static final String TENANT = "tenant-a";

    protected abstract DeviceRepository repository();
    protected abstract Uni<?> resetRepository();

    @Test
    @RunOnVertxContext
    void savesAndFindsCompleteDevice(UniAsserter asserter) {
        String vin = "1HGCM82633A004352";
        asserter.execute(this::resetRepository);
        asserter.assertThat(() -> repository().save(TENANT, newDevice(vin)), saved -> {
            assertEquals(vin, saved.vin());
            assertEquals(Map.of("gps", "true", "can", "v2"), saved.capabilities());
            assertEquals(DeviceStatus.IDLE, saved.status());
            assertNotNull(saved.registeredAt());
            assertNotNull(saved.updatedAt());
        });
        asserter.assertThat(() -> repository().findByVin(TENANT, vin), found -> {
            assertTrue(found.isPresent());
            assertEquals(vin, found.orElseThrow().vin());
            assertEquals(Map.of("gps", "true", "can", "v2"), found.orElseThrow().capabilities());
        });
    }

    @Test
    @RunOnVertxContext
    void returnsEmptyForUnknownDevice(UniAsserter asserter) {
        asserter.execute(this::resetRepository);
        asserter.assertThat(() -> repository().findByVin(TENANT, "WVWZZZ1JZXW000001"),
            found -> assertFalse(found.isPresent()));
    }

    @Test
    @RunOnVertxContext
    void updatesStatusAndReturnsUpdatedDevice(UniAsserter asserter) {
        String vin = "JH4TB2H26CC000001";
        asserter.execute(this::resetRepository);
        asserter.execute(() -> repository().save(TENANT, newDevice(vin)));
        asserter.assertThat(() -> repository().updateStatus(TENANT, vin, DeviceStatus.ACTIVE), updated -> {
            assertTrue(updated.isPresent());
            assertEquals(DeviceStatus.ACTIVE, updated.orElseThrow().status());
        });
        asserter.assertThat(() -> repository().findByVin(TENANT, vin), found ->
            assertEquals(DeviceStatus.ACTIVE, found.orElseThrow().status()));
    }

    @Test
    @RunOnVertxContext
    void returnsEmptyWhenUpdatingUnknownDevice(UniAsserter asserter) {
        asserter.execute(this::resetRepository);
        asserter.assertThat(
            () -> repository().updateStatus(TENANT, "1M8GDM9AXKP042788", DeviceStatus.MAINTENANCE),
            updated -> assertTrue(updated.isEmpty()));
    }

    @Test
    @RunOnVertxContext
    void isolatesTheSameVinAcrossTenants(UniAsserter asserter) {
        String vin = "1FTFW1ET4EFA00001";
        asserter.execute(this::resetRepository);
        asserter.execute(() -> repository().save("tenant-a", newDevice(vin)));
        asserter.assertThat(() -> repository().findByVin("tenant-b", vin),
            found -> assertTrue(found.isEmpty()));
        asserter.execute(() -> repository().save("tenant-b", newDevice(vin)));
        asserter.assertThat(() -> repository().findByVin("tenant-a", vin),
            found -> assertTrue(found.isPresent()));
        asserter.assertThat(() -> repository().findByVin("tenant-b", vin),
            found -> assertTrue(found.isPresent()));
    }

    private static Device newDevice(String vin) {
        return Device.registerNew(vin, "OBD", "FleetIQ", "Edge", 2025,
            Map.of("gps", "true", "can", "v2"), 2026);
    }
}
