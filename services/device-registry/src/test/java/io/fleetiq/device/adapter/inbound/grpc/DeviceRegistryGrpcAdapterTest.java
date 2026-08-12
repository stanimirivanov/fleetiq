package io.fleetiq.device.adapter.inbound.grpc;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase;
import io.fleetiq.proto.device.v1.GetDeviceRequest;
import io.fleetiq.proto.device.v1.RegisterDeviceRequest;
import io.fleetiq.proto.device.v1.UpdateDeviceStatusRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
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
            new DeviceRegistryUseCase.UpdateStatusResult.Updated(device)), new GrpcDeviceMapper());

        var response = adapter.registerDevice(RegisterDeviceRequest.newBuilder()
            .setVin(VIN).setDeviceType("OBD").setManufacturer("FleetIQ")
            .setModel("Edge").setYear(2025).build()).await().indefinitely();

        assertEquals(VIN, response.getDevice().getVin());
    }

    @Test
    void mapsExpectedOutcomesToGrpcStatuses() {
        var adapter = new DeviceRegistryGrpcAdapter(new StubUseCase(
            new DeviceRegistryUseCase.RegisterResult.AlreadyExists(VIN), Optional.empty(),
            new DeviceRegistryUseCase.UpdateStatusResult.NotFound(VIN)), new GrpcDeviceMapper());

        assertStatus(Status.Code.ALREADY_EXISTS, () -> adapter.registerDevice(
            RegisterDeviceRequest.newBuilder().setVin(VIN).build()).await().indefinitely());
        assertStatus(Status.Code.NOT_FOUND, () -> adapter.getDevice(
            GetDeviceRequest.newBuilder().setVin(VIN).build()).await().indefinitely());
        assertStatus(Status.Code.NOT_FOUND, () -> adapter.updateDeviceStatus(
            UpdateDeviceStatusRequest.newBuilder().setVin(VIN)
                .setStatus(io.fleetiq.proto.device.v1.DeviceStatus.DEVICE_STATUS_ACTIVE)
                .build()).await().indefinitely());
    }

    @Test
    void mapsUnspecifiedStatusToInvalidArgument() {
        var adapter = new DeviceRegistryGrpcAdapter(new StubUseCase(null, Optional.empty(), null),
            new GrpcDeviceMapper());

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

    private record StubUseCase(
        RegisterResult registerResult,
        Optional<Device> device,
        UpdateStatusResult updateResult
    ) implements DeviceRegistryUseCase {
        @Override public Uni<RegisterResult> register(RegisterCommand command) {
            return Uni.createFrom().item(registerResult);
        }
        @Override public Uni<Optional<Device>> getByVin(String vin) {
            return Uni.createFrom().item(device);
        }
        @Override public Uni<UpdateStatusResult> updateStatus(UpdateStatusCommand command) {
            return Uni.createFrom().item(updateResult);
        }
    }
}
