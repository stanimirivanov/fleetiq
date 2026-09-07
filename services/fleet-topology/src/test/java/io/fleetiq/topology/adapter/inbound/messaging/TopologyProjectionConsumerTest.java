package io.fleetiq.topology.adapter.inbound.messaging;

import io.fleetiq.proto.events.v1.DeviceProjectionEvent;
import io.fleetiq.proto.events.v1.PositionProjectionEvent;
import io.fleetiq.topology.domain.model.VehicleProjection;
import io.fleetiq.topology.domain.port.inbound.TopologyProjectionUseCase;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TopologyProjectionConsumerTest {

    private final StubUseCase useCase = new StubUseCase();
    private final StubQuarantine quarantine = new StubQuarantine();
    private final TopologyProjectionConsumer consumer = new TopologyProjectionConsumer(useCase, quarantine);

    @Test
    void projectsDeviceEvent() {
        consumer.consumeDevice(DeviceProjectionEvent.newBuilder()
            .setVin("1HGCM82633A004352").setDeviceType("OBD").setStatus("ACTIVE")
            .setTenantId("tenant-a")
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
            .setTenantId("tenant-a")
            .setAltitude(34).setObservedAtEpochMillis(1_765_000_000_000L)
            .build().toByteArray()).await().indefinitely();

        assertEquals("1HGCM82633A004352", useCase.positionVin);
        assertEquals(52.52, useCase.latitude);
        assertEquals(13.405, useCase.longitude);
    }

    @Test
    void quarantinesMalformedEventsAndCoordinatesWithoutBrokerRedelivery() {
        byte[] malformed = new byte[]{1, 2, 3};
        consumer.consumeDevice(malformed).await().indefinitely();
        assertEquals("device-projection.v1", quarantine.eventType);
        assertArrayEquals(malformed, quarantine.payload);
        assertEquals(1, quarantine.attempts);

        byte[] invalidPosition = PositionProjectionEvent.newBuilder()
            .setTenantId("tenant-a").setVin("1HGCM82633A004352").setLatitude(91).build().toByteArray();
        consumer.consumePosition(invalidPosition).await().indefinitely();
        assertEquals("position-projection.v1", quarantine.eventType);
        assertEquals(1, quarantine.attempts);
    }

    @Test
    void retriesTransientProjectionFailureThreeTimesThenQuarantines() {
        useCase.fail = true;
        consumer.consumeDevice(DeviceProjectionEvent.newBuilder()
            .setTenantId("tenant-a").setVin("1HGCM82633A004352")
            .setOccurredAtEpochMillis(1_765_000_000_000L).build().toByteArray())
            .await().indefinitely();

        assertEquals(3, useCase.calls);
        assertEquals(3, quarantine.attempts);
    }

    private static final class StubUseCase implements TopologyProjectionUseCase {
        private VehicleProjection vehicle;
        private String positionVin;
        private double latitude;
        private double longitude;
        private int calls;
        private boolean fail;

        @Override
        public Uni<Void> projectDevice(String tenantId, VehicleProjection vehicle) {
            calls++;
            if (fail) return Uni.createFrom().failure(new IllegalStateException("database unavailable"));
            this.vehicle = vehicle;
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<Void> projectPosition(String tenantId, String vin, double latitude, double longitude, double altitude,
                                         Instant observedAt) {
            this.positionVin = vin;
            this.latitude = latitude;
            this.longitude = longitude;
            return Uni.createFrom().voidItem();
        }
    }

    private static final class StubQuarantine
        implements io.fleetiq.topology.domain.port.outbound.ProjectionQuarantine {
        private String eventType;
        private byte[] payload;
        private int attempts;

        @Override
        public Uni<Void> quarantine(String eventType, byte[] payload, Throwable failure, int attempts) {
            this.eventType = eventType;
            this.payload = payload;
            this.attempts = attempts;
            return Uni.createFrom().voidItem();
        }
    }
}
