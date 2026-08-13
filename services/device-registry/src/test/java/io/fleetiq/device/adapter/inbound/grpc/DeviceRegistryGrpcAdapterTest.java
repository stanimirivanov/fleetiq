package io.fleetiq.device.adapter.inbound.grpc;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase;
import io.fleetiq.proto.device.v1.GetDeviceRequest;
import io.fleetiq.proto.device.v1.RegisterDeviceRequest;
import io.fleetiq.proto.device.v1.UpdateDeviceStatusRequest;
import io.fleetiq.proto.device.v1.EnrollDeviceRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import io.fleetiq.security.CurrentTenant;
import io.fleetiq.security.TenantIdentity;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceRegistryGrpcAdapterTest {

    private static final String VIN = "1HGCM82633A004352";

    @Test
    void mapsRegistrationResponse() {
        Device device = device();
        var adapter = new DeviceRegistryGrpcAdapter(new StubUseCase(
            new DeviceRegistryUseCase.RegisterResult.Registered(device), Optional.of(device),
            new DeviceRegistryUseCase.UpdateStatusResult.Updated(device),
            new DeviceRegistryUseCase.EnrollResult.Enrolled("tenant-a/" + VIN, "secret")),
            new GrpcDeviceMapper(), tenant());

        var response = adapter.registerDevice(RegisterDeviceRequest.newBuilder()
            .setVin(VIN).setDeviceType("OBD").setManufacturer("FleetIQ")
            .setModel("Edge").setYear(2025).build()).await().indefinitely();

        assertEquals(VIN, response.getDevice().getVin());
    }

    @Test
    void propagatesAuthenticatedTenantIntoTheUseCase() {
        var tenantSeen = new java.util.concurrent.atomic.AtomicReference<String>();
        Device device = device();
        DeviceRegistryUseCase useCase = new DeviceRegistryUseCase() {
            @Override public Uni<RegisterResult> register(RegisterCommand command) {
                tenantSeen.set(command.tenantId());
                return Uni.createFrom().item(new RegisterResult.Registered(device));
            }
            @Override public Uni<Optional<Device>> getByVin(String tenantId, String vin) {
                return Uni.createFrom().item(Optional.empty());
            }
            @Override public Uni<UpdateStatusResult> updateStatus(UpdateStatusCommand command) {
                return Uni.createFrom().item(new UpdateStatusResult.NotFound(command.vin()));
            }
            @Override public Uni<EnrollResult> enroll(EnrollCommand command) {
                return Uni.createFrom().item(new EnrollResult.NotFound(command.vin()));
            }
        };
        var adapter = new DeviceRegistryGrpcAdapter(useCase, new GrpcDeviceMapper(), tenant());

        adapter.registerDevice(RegisterDeviceRequest.newBuilder().setVin(VIN).build())
            .await().indefinitely();

        assertEquals("tenant-a", tenantSeen.get());
    }

    @Test
    void mapsExpectedOutcomesToGrpcStatuses() {
        var adapter = new DeviceRegistryGrpcAdapter(new StubUseCase(
            new DeviceRegistryUseCase.RegisterResult.AlreadyExists(VIN), Optional.empty(),
            new DeviceRegistryUseCase.UpdateStatusResult.NotFound(VIN),
            new DeviceRegistryUseCase.EnrollResult.NotFound(VIN)), new GrpcDeviceMapper(), tenant());

        assertStatus(Status.Code.ALREADY_EXISTS, () -> adapter.registerDevice(
            RegisterDeviceRequest.newBuilder().setVin(VIN).build()).await().indefinitely());
        assertStatus(Status.Code.NOT_FOUND, () -> adapter.getDevice(
            GetDeviceRequest.newBuilder().setVin(VIN).build()).await().indefinitely());
        assertStatus(Status.Code.NOT_FOUND, () -> adapter.updateDeviceStatus(
            UpdateDeviceStatusRequest.newBuilder().setVin(VIN)
                .setStatus(io.fleetiq.proto.device.v1.DeviceStatus.DEVICE_STATUS_ACTIVE)
                .build()).await().indefinitely());
        assertStatus(Status.Code.NOT_FOUND, () -> adapter.enrollDevice(
            EnrollDeviceRequest.newBuilder().setVin(VIN).build()).await().indefinitely());
    }

    @Test
    void returnsCredentialOnlyFromExplicitEnrollment() {
        var adapter = new DeviceRegistryGrpcAdapter(new StubUseCase(
            null, Optional.of(device()), null,
            new DeviceRegistryUseCase.EnrollResult.Enrolled("tenant-a/" + VIN, "one-time-secret")),
            new GrpcDeviceMapper(), tenant());

        var response = adapter.enrollDevice(EnrollDeviceRequest.newBuilder().setVin(VIN).build())
            .await().indefinitely();

        assertEquals("tenant-a/" + VIN, response.getUsername());
        assertEquals("one-time-secret", response.getPassword());
    }

    @Test
    void mapsUnspecifiedStatusToInvalidArgument() {
        var adapter = new DeviceRegistryGrpcAdapter(new StubUseCase(null, Optional.empty(), null, null),
            new GrpcDeviceMapper(), tenant());

        assertStatus(Status.Code.INVALID_ARGUMENT, () -> adapter.updateDeviceStatus(
            UpdateDeviceStatusRequest.newBuilder().setVin(VIN).build()).await().indefinitely());
    }

    private static void assertStatus(Status.Code expected, Runnable operation) {
        StatusRuntimeException failure = assertThrows(StatusRuntimeException.class, operation::run);
        assertEquals(expected, failure.getStatus().getCode());
    }

    private static Device device() {
        return Device.registerNew(VIN, "OBD", "FleetIQ", "Edge", 2025, Map.of(), 2026);
    }

    private static CurrentTenant tenant() {
        return new CurrentTenant() {
            @Override public TenantIdentity get() {
                return new TenantIdentity("tenant-a", "test", java.util.Set.of("operator"));
            }
        };
    }

    private record StubUseCase(
        RegisterResult registerResult,
        Optional<Device> device,
        UpdateStatusResult updateResult,
        EnrollResult enrollResult
    ) implements DeviceRegistryUseCase {
        @Override public Uni<RegisterResult> register(RegisterCommand command) {
            return Uni.createFrom().item(registerResult);
        }
        @Override public Uni<Optional<Device>> getByVin(String tenantId, String vin) {
            return Uni.createFrom().item(device);
        }
        @Override public Uni<UpdateStatusResult> updateStatus(UpdateStatusCommand command) {
            return Uni.createFrom().item(updateResult);
        }
        @Override public Uni<EnrollResult> enroll(EnrollCommand command) {
            return Uni.createFrom().item(enrollResult);
        }
    }
}
