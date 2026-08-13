package io.fleetiq.device.adapter.inbound.grpc;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.fleetiq.device.domain.model.DeviceValidationException;
import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase.RegisterCommand;
import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase.UpdateStatusCommand;
import io.fleetiq.proto.common.v1.Timestamp;
import io.fleetiq.proto.device.v1.GetDeviceResponse;
import io.fleetiq.proto.device.v1.RegisterDeviceRequest;
import io.fleetiq.proto.device.v1.RegisterDeviceResponse;
import io.fleetiq.proto.device.v1.UpdateDeviceStatusRequest;
import io.fleetiq.proto.device.v1.UpdateDeviceStatusResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

@ApplicationScoped
public class GrpcDeviceMapper {

    public RegisterCommand toCommand(String tenantId, RegisterDeviceRequest request) {
        return new RegisterCommand(
            tenantId,
            request.getVin(),
            request.getDeviceType(),
            request.getManufacturer(),
            request.getModel(),
            request.getYear(),
            request.getCapabilitiesMap()
        );
    }

    public UpdateStatusCommand toCommand(String tenantId, UpdateDeviceStatusRequest request) {
        return new UpdateStatusCommand(tenantId, request.getVin(), toDomain(request.getStatus()));
    }

    public RegisterDeviceResponse toRegisterResponse(Device device) {
        return RegisterDeviceResponse.newBuilder().setDevice(toProto(device)).build();
    }

    public GetDeviceResponse toGetResponse(Device device) {
        return GetDeviceResponse.newBuilder().setDevice(toProto(device)).build();
    }

    public UpdateDeviceStatusResponse toUpdateResponse(Device device) {
        return UpdateDeviceStatusResponse.newBuilder().setDevice(toProto(device)).build();
    }

    io.fleetiq.proto.device.v1.Device toProto(Device device) {
        var builder = io.fleetiq.proto.device.v1.Device.newBuilder()
            .setVin(device.vin())
            .setDeviceType(device.deviceType())
            .setManufacturer(device.manufacturer())
            .setModel(device.model())
            .setYear(device.year())
            .putAllCapabilities(device.capabilities())
            .setStatus(toProto(device.status()));
        if (device.registeredAt() != null) {
            builder.setRegisteredAt(toProto(device.registeredAt()));
        }
        return builder.build();
    }

    private DeviceStatus toDomain(io.fleetiq.proto.device.v1.DeviceStatus status) {
        return switch (status) {
            case DEVICE_STATUS_IDLE -> DeviceStatus.IDLE;
            case DEVICE_STATUS_ACTIVE -> DeviceStatus.ACTIVE;
            case DEVICE_STATUS_MAINTENANCE -> DeviceStatus.MAINTENANCE;
            case DEVICE_STATUS_DECOMMISSIONED -> DeviceStatus.DECOMMISSIONED;
            case DEVICE_STATUS_UNSPECIFIED, UNRECOGNIZED ->
                throw new DeviceValidationException("A recognized device status is required");
        };
    }

    private io.fleetiq.proto.device.v1.DeviceStatus toProto(DeviceStatus status) {
        return switch (status) {
            case IDLE -> io.fleetiq.proto.device.v1.DeviceStatus.DEVICE_STATUS_IDLE;
            case ACTIVE -> io.fleetiq.proto.device.v1.DeviceStatus.DEVICE_STATUS_ACTIVE;
            case MAINTENANCE -> io.fleetiq.proto.device.v1.DeviceStatus.DEVICE_STATUS_MAINTENANCE;
            case DECOMMISSIONED -> io.fleetiq.proto.device.v1.DeviceStatus.DEVICE_STATUS_DECOMMISSIONED;
        };
    }

    private Timestamp toProto(Instant instant) {
        return Timestamp.newBuilder().setEpochMillis(instant.toEpochMilli()).build();
    }
}
