package io.fleetiq.device.domain.port.inbound;

import io.fleetiq.device.domain.model.Device;
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

    Uni<Device> register(RegisterCommand command);
    Uni<Optional<Device>> getByVin(String vin);
    Uni<Device> updateStatus(String vin, String status);
}
