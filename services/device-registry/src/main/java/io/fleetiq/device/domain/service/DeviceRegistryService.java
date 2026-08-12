package io.fleetiq.device.domain.service;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase;
import io.fleetiq.device.domain.port.outbound.DeviceRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class DeviceRegistryService implements DeviceRegistryUseCase {

    private final DeviceRepository repository;

    @Override
    public Uni<Device> register(RegisterCommand command) {
        log.info("Registering device: {}", command.vin());

        // Domain model handles default status & initial state
        Device newDevice = Device.registerNew(
            command.vin(),
            command.deviceType(),
            command.manufacturer(),
            command.model(),
            command.year(),
            command.capabilities()
        );

        return repository.save(newDevice)
            .invoke(d -> log.info("Device registered successfully: {}", d.vin()));
    }

    @Override
    public Uni<Optional<Device>> getByVin(String vin) {
        return repository.findByVin(vin);
    }

    @Override
    public Uni<Device> updateStatus(String vin, String status) {
        log.info("Updating device status: {} -> {}", vin, status);
        // TODO: Validate or parse string to DeviceStatus enum here if needed
        return repository.updateStatus(vin, status);
    }
}
