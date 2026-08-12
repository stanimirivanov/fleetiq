package io.fleetiq.device.domain.port.inbound;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.smallrye.mutiny.Uni;
import lombok.Builder;

import java.util.Map;
import java.util.Optional;

public interface DeviceRegistryUseCase {

    @Builder
    record RegisterCommand(
        String vin,
        String deviceType,
        String manufacturer,
        String model,
        int year,
        Map<String, String> capabilities
    ) {}

    record UpdateStatusCommand(String vin, DeviceStatus status) {}

    sealed interface RegisterResult {
        record Registered(Device device) implements RegisterResult {}
        record AlreadyExists(String vin) implements RegisterResult {}
    }

    sealed interface UpdateStatusResult {
        record Updated(Device device) implements UpdateStatusResult {}
        record NotFound(String vin) implements UpdateStatusResult {}
    }

    Uni<RegisterResult> register(RegisterCommand command);
    Uni<Optional<Device>> getByVin(String vin);
    Uni<UpdateStatusResult> updateStatus(UpdateStatusCommand command);
}
