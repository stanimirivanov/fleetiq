package io.fleetiq.device.domain.service;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase;
import io.fleetiq.device.domain.port.outbound.DeviceRepository;
import io.fleetiq.device.domain.port.outbound.DeviceCredentialProvisioner;
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
    private final Clock clock;
    private final DeviceCredentialProvisioner credentialProvisioner;

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

        validateTenantId(command.tenantId());
        return repository.findByVin(command.tenantId(), command.vin())
            .onItem().transformToUni(existing -> existing.isPresent()
                ? Uni.createFrom().item(new RegisterResult.AlreadyExists(command.vin()))
                : repository.save(command.tenantId(), newDevice).map(RegisterResult.Registered::new));
    }

    @Override
    public Uni<Optional<Device>> getByVin(String tenantId, String vin) {
        validateTenantId(tenantId);
        Device.validateVin(vin);
        return repository.findByVin(tenantId, vin);
    }

    @Override
    public Uni<UpdateStatusResult> updateStatus(UpdateStatusCommand command) {
        Device.validateVin(command.vin());
        validateTenantId(command.tenantId());
        if (command.status() == null) {
            throw new io.fleetiq.device.domain.model.DeviceValidationException("Status is required");
        }
        return repository.updateStatus(command.tenantId(), command.vin(), command.status())
            .map(device -> device
                .<UpdateStatusResult>map(UpdateStatusResult.Updated::new)
                .orElseGet(() -> new UpdateStatusResult.NotFound(command.vin())));
    }

    @Override
    public Uni<EnrollResult> enroll(EnrollCommand command) {
        validateTenantId(command.tenantId());
        Device.validateVin(command.vin());
        return repository.findByVin(command.tenantId(), command.vin())
            .onItem().transformToUni(device -> {
                if (device.isEmpty()) {
                    return Uni.createFrom().item(new EnrollResult.NotFound(command.vin()));
                }
                if (device.get().status() == DeviceStatus.DECOMMISSIONED) {
                    return Uni.createFrom().item(new EnrollResult.Decommissioned(command.vin()));
                }
                return credentialProvisioner.provision(command.tenantId(), command.vin())
                    .map(credential -> (EnrollResult) new EnrollResult.Enrolled(
                        credential.username(), credential.secret()));
            });
    }

    private static void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new io.fleetiq.device.domain.model.DeviceValidationException("Tenant ID is required");
        }
    }
}
