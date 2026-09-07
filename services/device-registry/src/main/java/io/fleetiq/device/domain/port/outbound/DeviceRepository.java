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
    /** Returns the device owned by the tenant, or an empty value when it is not registered. */
    Uni<Optional<Device>> findByVin(String tenantId, String vin);

    /**
     * Stores a new device and its topology projection event in one database transaction.
     */
    Uni<Device> save(String tenantId, Device device);

    /**
     * Updates the status and projection event atomically, returning an empty value for an unknown
     * tenant/VIN pair.
     */
    Uni<Optional<Device>> updateStatus(String tenantId, String vin, DeviceStatus status);
}
