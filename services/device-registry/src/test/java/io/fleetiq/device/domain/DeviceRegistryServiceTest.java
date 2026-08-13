package io.fleetiq.device.domain;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.fleetiq.device.domain.model.DeviceValidationException;
import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase;
import io.fleetiq.device.domain.port.outbound.DeviceRepository;
import io.fleetiq.device.domain.port.outbound.DeviceEventPublisher;
import io.fleetiq.device.domain.service.DeviceRegistryService;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceRegistryServiceTest {

    private static final String VIN = "1HGCM82633A004352";
    private final StubRepository repository = new StubRepository();
    private final DeviceRegistryService service = new DeviceRegistryService(
        repository,
        device -> Uni.createFrom().voidItem(),
        Clock.fixed(Instant.parse("2026-08-12T12:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void registersValidatedDeviceWithDomainDefaultStatus() {
        var result = service.register(command(VIN)).await().indefinitely();

        var registered = assertInstanceOf(DeviceRegistryUseCase.RegisterResult.Registered.class, result);
        assertEquals(DeviceStatus.IDLE, registered.device().status());
        assertEquals(Map.of("gps", "enabled"), registered.device().capabilities());
    }

    @Test
    void reportsDuplicateAsExpectedOutcome() {
        repository.device = Optional.of(device(VIN));

        var result = service.register(command(VIN)).await().indefinitely();

        assertInstanceOf(DeviceRegistryUseCase.RegisterResult.AlreadyExists.class, result);
    }

    @Test
    void rejectsInvalidVinAndFutureYear() {
        assertThrows(DeviceValidationException.class,
            () -> service.register(command("invalid")).await().indefinitely());
        var future = new DeviceRegistryUseCase.RegisterCommand(
            VIN, "OBD", "FleetIQ", "Edge", 2028, Map.of());
        assertThrows(DeviceValidationException.class,
            () -> service.register(future).await().indefinitely());
    }

    @Test
    void reportsMissingStatusUpdateAsExpectedOutcome() {
        var result = service.updateStatus(
            new DeviceRegistryUseCase.UpdateStatusCommand(VIN, DeviceStatus.MAINTENANCE)
        ).await().indefinitely();

        assertInstanceOf(DeviceRegistryUseCase.UpdateStatusResult.NotFound.class, result);
    }

    private static DeviceRegistryUseCase.RegisterCommand command(String vin) {
        return new DeviceRegistryUseCase.RegisterCommand(
            vin, "OBD", "FleetIQ", "Edge", 2025, Map.of("gps", "enabled"));
    }

    private static Device device(String vin) {
        return Device.registerNew(vin, "OBD", "FleetIQ", "Edge", 2025, Map.of(), 2026);
    }

    private static final class StubRepository implements DeviceRepository {
        private Optional<Device> device = Optional.empty();

        @Override
        public Uni<Optional<Device>> findByVin(String vin) {
            return Uni.createFrom().item(device);
        }

        @Override
        public Uni<Device> save(Device device) {
            this.device = Optional.of(device);
            return Uni.createFrom().item(device);
        }

        @Override
        public Uni<Optional<Device>> updateStatus(String vin, DeviceStatus status) {
            return Uni.createFrom().item(device.map(value -> new Device(
                value.vin(), value.deviceType(), value.manufacturer(), value.model(), value.year(),
                value.capabilities(), status, value.registeredAt(), value.updatedAt())));
        }
    }
}
