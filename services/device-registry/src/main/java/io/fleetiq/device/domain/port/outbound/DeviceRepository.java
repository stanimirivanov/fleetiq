package io.fleetiq.device.domain.port.outbound;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.smallrye.mutiny.Uni;

import java.util.Optional;

public interface DeviceRepository {
    Uni<Optional<Device>> findByVin(String vin);
    Uni<Device> save(Device device);
    Uni<Optional<Device>> updateStatus(String vin, DeviceStatus status);
}
