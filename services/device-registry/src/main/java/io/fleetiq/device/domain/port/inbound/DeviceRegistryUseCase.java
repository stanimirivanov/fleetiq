package io.fleetiq.device.domain.port.inbound;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.smallrye.mutiny.Uni;
import lombok.Builder;

import java.util.Map;
import java.util.Optional;

/**
 * Inbound application boundary for registering devices, reading their metadata,
 * and changing their lifecycle status. Expected business outcomes are represented
 * by result types rather than exceptions.
 */
public interface DeviceRegistryUseCase {

    @Builder
    record RegisterCommand(
        String tenantId,
        String vin,
        String deviceType,
        String manufacturer,
        String model,
        int year,
        Map<String, String> capabilities
    ) {}

    record UpdateStatusCommand(String tenantId, String vin, DeviceStatus status) {}

    sealed interface RegisterResult {
        record Registered(Device device) implements RegisterResult {}
        record AlreadyExists(String vin) implements RegisterResult {}
    }

    sealed interface UpdateStatusResult {
        record Updated(Device device) implements UpdateStatusResult {}
        record NotFound(String vin) implements UpdateStatusResult {}
    }

    /** Registers a device, returning {@code AlreadyExists} when the VIN is already known. */
    Uni<RegisterResult> register(RegisterCommand command);

    /** Returns an empty optional when no device has the supplied VIN. */
    Uni<Optional<Device>> getByVin(String tenantId, String vin);

    /** Changes status, returning {@code NotFound} when the device does not exist. */
    Uni<UpdateStatusResult> updateStatus(UpdateStatusCommand command);
}
