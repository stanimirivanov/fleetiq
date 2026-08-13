package io.fleetiq.topology.adapter.inbound.messaging;

import io.fleetiq.proto.events.v1.DeviceProjectionEvent;
import io.fleetiq.proto.events.v1.PositionProjectionEvent;
import io.fleetiq.topology.domain.model.VehicleProjection;
import io.fleetiq.topology.domain.port.inbound.TopologyProjectionUseCase;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TopologyProjectionConsumerTest {

    private final StubUseCase useCase = new StubUseCase();
    private final TopologyProjectionConsumer consumer = new TopologyProjectionConsumer(useCase);

    @Test
    void projectsDeviceEvent() {
        consumer.consumeDevice(DeviceProjectionEvent.newBuilder()
            .setVin("1HGCM82633A004352").setDeviceType("OBD").setStatus("ACTIVE")
            .setOccurredAtEpochMillis(1_765_000_000_000L).build().toByteArray())
            .await().indefinitely();

        assertEquals("1HGCM82633A004352", useCase.vehicle.vin());
        assertEquals("OBD", useCase.vehicle.deviceType());
        assertEquals("ACTIVE", useCase.vehicle.status());
    }

    @Test
    void projectsPositionEvent() {
        consumer.consumePosition(PositionProjectionEvent.newBuilder()
            .setVin("1HGCM82633A004352").setLatitude(52.52).setLongitude(13.405)
            .setAltitude(34).setObservedAtEpochMillis(1_765_000_000_000L)
            .build().toByteArray()).await().indefinitely();

        assertEquals("1HGCM82633A004352", useCase.positionVin);
        assertEquals(52.52, useCase.latitude);
        assertEquals(13.405, useCase.longitude);
    }

    @Test
    void rejectsMalformedEventsAndCoordinates() {
        assertThrows(IllegalArgumentException.class,
            () -> consumer.consumeDevice(new byte[]{1, 2, 3}).await().indefinitely());
        byte[] invalidPosition = PositionProjectionEvent.newBuilder()
            .setVin("1HGCM82633A004352").setLatitude(91).build().toByteArray();
        assertThrows(IllegalArgumentException.class,
            () -> consumer.consumePosition(invalidPosition).await().indefinitely());
    }

    private static final class StubUseCase implements TopologyProjectionUseCase {
        private VehicleProjection vehicle;
        private String positionVin;
        private double latitude;
        private double longitude;

        @Override
        public Uni<Void> projectDevice(VehicleProjection vehicle) {
            this.vehicle = vehicle;
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<Void> projectPosition(String vin, double latitude, double longitude, double altitude,
                                         Instant observedAt) {
            this.positionVin = vin;
            this.latitude = latitude;
            this.longitude = longitude;
            return Uni.createFrom().voidItem();
        }
    }
}
