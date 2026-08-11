package io.fleetiq.device.domain.port.outbound;

import io.fleetiq.device.domain.model.Device;
import java.util.Optional;

public interface DeviceRepository {
    void save(Device device);
    Optional<Device> findByVin(String vin);
    void updateStatus(String vin, String status);
}
