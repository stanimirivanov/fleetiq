package io.fleetiq.topology.adapter.inbound.messaging;

import com.google.protobuf.InvalidProtocolBufferException;
import io.fleetiq.proto.events.v1.DeviceProjectionEvent;
import io.fleetiq.proto.events.v1.PositionProjectionEvent;
import io.fleetiq.topology.domain.model.VehicleProjection;
import io.fleetiq.topology.domain.port.inbound.TopologyProjectionUseCase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.time.Instant;

@ApplicationScoped
@RequiredArgsConstructor
public class TopologyProjectionConsumer {

    private final TopologyProjectionUseCase useCase;

    @Incoming("device-projections-in")
    public Uni<Void> consumeDevice(byte[] payload) {
        try {
            DeviceProjectionEvent event = DeviceProjectionEvent.parseFrom(payload);
            requireVin(event.getVin());
            return useCase.projectDevice(new VehicleProjection(
                event.getVin(), event.getDeviceType(), event.getStatus(), null, null, null,
                Instant.ofEpochMilli(event.getOccurredAtEpochMillis()), null));
        } catch (InvalidProtocolBufferException e) {
            return Uni.createFrom().failure(new IllegalArgumentException("Invalid device projection event", e));
        }
    }

    @Incoming("position-projections-in")
    public Uni<Void> consumePosition(byte[] payload) {
        try {
            PositionProjectionEvent event = PositionProjectionEvent.parseFrom(payload);
            requireVin(event.getVin());
            validateCoordinates(event.getLatitude(), event.getLongitude());
            return useCase.projectPosition(event.getVin(), event.getLatitude(), event.getLongitude(),
                event.getAltitude(), Instant.ofEpochMilli(event.getObservedAtEpochMillis()));
        } catch (InvalidProtocolBufferException e) {
            return Uni.createFrom().failure(new IllegalArgumentException("Invalid position projection event", e));
        }
    }

    private static void requireVin(String vin) {
        if (vin == null || vin.isBlank()) throw new IllegalArgumentException("VIN is required");
    }

    private static void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }
    }
}
