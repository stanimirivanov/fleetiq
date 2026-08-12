package io.fleetiq.streaming.adapter.inbound.grpc;

import io.fleetiq.proto.common.v1.VehicleStatus;
import io.fleetiq.streaming.domain.model.PositionEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrpcPositionMapperTest {

    @Test
    void mapsDomainEventToGrpcUpdate() {
        var event = new PositionEvent(
            "1HGCM82633A004352", Instant.parse("2026-08-12T12:00:00Z"),
            52.52, 13.405, 34, 72.5, "MOVING");

        var update = new GrpcPositionMapper().toProto(event);

        assertEquals(event.vin(), update.getVin());
        assertEquals(VehicleStatus.VEHICLE_STATUS_MOVING, update.getStatus());
        assertEquals(event.observedAt().toEpochMilli(), update.getTimestamp().getEpochMillis());
    }
}
