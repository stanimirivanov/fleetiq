package io.fleetiq.device.domain.service;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase;
import io.fleetiq.device.domain.port.outbound.DeviceRepository;
import io.fleetiq.device.domain.port.outbound.DeviceEventPublisher;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.time.Clock;
import java.time.Year;

@ApplicationScoped
@RequiredArgsConstructor
public class DeviceRegistryService implements DeviceRegistryUseCase {

    private final DeviceRepository repository;
    private final DeviceEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public Uni<RegisterResult> register(RegisterCommand command) {
        Device.validateVin(command.vin());
        Device newDevice = Device.registerNew(
            command.vin(),
            command.deviceType(),
            command.manufacturer(),
            command.model(),
            command.year(),
            command.capabilities(),
            Year.now(clock).getValue()
        );

        return repository.findByVin(command.vin())
            .onItem().transformToUni(existing -> existing.isPresent()
                ? Uni.createFrom().item(new RegisterResult.AlreadyExists(command.vin()))
                : repository.save(newDevice)
                    .call(eventPublisher::publish)
                    .map(RegisterResult.Registered::new));
    }

    @Override
    public Uni<Optional<Device>> getByVin(String vin) {
        Device.validateVin(vin);
        return repository.findByVin(vin);
    }

    @Override
    public Uni<UpdateStatusResult> updateStatus(UpdateStatusCommand command) {
        Device.validateVin(command.vin());
        if (command.status() == null) {
            throw new io.fleetiq.device.domain.model.DeviceValidationException("Status is required");
        }
        return repository.updateStatus(command.vin(), command.status())
            .call(device -> device.isPresent()
                ? eventPublisher.publish(device.orElseThrow())
                : Uni.createFrom().voidItem())
            .map(device -> device
                .<UpdateStatusResult>map(UpdateStatusResult.Updated::new)
                .orElseGet(() -> new UpdateStatusResult.NotFound(command.vin())));
    }
}
