package io.fleetiq.device.domain.port.inbound;

import io.fleetiq.device.domain.model.Device;
import java.util.Map;

public interface DeviceRegistryUseCase {

    record RegisterCommand(
        String vin,
        String deviceType,
        String manufacturer,
        String model,
        int year,
        Map<String, String> capabilities
    ) {}

    Device register(RegisterCommand command);
    Device getByVin(String vin);
    Device updateStatus(String vin, String status);
}
