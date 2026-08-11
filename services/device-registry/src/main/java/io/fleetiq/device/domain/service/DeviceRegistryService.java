package io.fleetiq.device.domain.service;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase;
import io.fleetiq.device.domain.port.outbound.DeviceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class DeviceRegistryService implements DeviceRegistryUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryService.class);
    private final DeviceRepository repository;

    public DeviceRegistryService(DeviceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Device register(RegisterCommand command) {
        log.debug("Registering device: {}", command.vin());
        var device = new Device(
            command.vin(), command.deviceType(), command.manufacturer(),
            command.model(), command.year(), command.capabilities(),
            "IDLE", Instant.now(), Instant.now()
        );
        repository.save(device);
        return device;
    }

    @Override
    public Device getByVin(String vin) {
        return repository.findByVin(vin)
            .orElseThrow(() -> new IllegalArgumentException("Device not found: " + vin));
    }

    @Override
    public Device updateStatus(String vin, String status) {
        repository.updateStatus(vin, status);
        return getByVin(vin);
    }
}
