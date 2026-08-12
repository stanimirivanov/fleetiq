package io.fleetiq.device.domain.port.outbound;

import io.fleetiq.device.domain.model.Device;
import io.smallrye.mutiny.Uni;

import java.util.Optional;

public interface DeviceRepository {
    Uni<Optional<Device>> findByVin(String vin);
    Uni<Device> save(Device device);
    Uni<Device> updateStatus(String vin, String status);
}
