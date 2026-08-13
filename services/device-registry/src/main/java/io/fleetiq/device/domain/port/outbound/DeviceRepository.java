package io.fleetiq.device.domain.port.outbound;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.smallrye.mutiny.Uni;

import java.util.Optional;

/**
 * Reactive persistence boundary for the device aggregate. An implementation owns
 * transaction details and reports expected absence with {@link Optional}.
 */
public interface DeviceRepository {
    Uni<Optional<Device>> findByVin(String vin);
    Uni<Device> save(Device device);
    Uni<Optional<Device>> updateStatus(String vin, DeviceStatus status);
}
