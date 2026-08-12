package io.fleetiq.device.adapter.inbound.grpc;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.model.DeviceStatus;
import io.fleetiq.device.domain.model.DeviceValidationException;
import io.fleetiq.proto.device.v1.UpdateDeviceStatusRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrpcDeviceMapperTest {

    private final GrpcDeviceMapper mapper = new GrpcDeviceMapper();

    @Test
    void mapsDeviceWithoutInventingTimestamp() {
        Device device = new Device(
            "1HGCM82633A004352", "OBD", "FleetIQ", "Edge", 2025,
            Map.of(), DeviceStatus.ACTIVE, Instant.parse("2026-08-12T12:00:00Z"), null);

        var proto = mapper.toProto(device);

        assertEquals(io.fleetiq.proto.device.v1.DeviceStatus.DEVICE_STATUS_ACTIVE, proto.getStatus());
        assertEquals(1786536000000L, proto.getRegisteredAt().getEpochMillis());
    }

    @Test
    void rejectsUnspecifiedStatus() {
        var request = UpdateDeviceStatusRequest.newBuilder()
            .setVin("1HGCM82633A004352")
            .build();

        assertThrows(DeviceValidationException.class, () -> mapper.toCommand(request));
    }
}
