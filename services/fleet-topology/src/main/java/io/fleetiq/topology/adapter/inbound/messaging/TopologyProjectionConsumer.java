package io.fleetiq.topology.adapter.inbound.messaging;

import com.google.protobuf.InvalidProtocolBufferException;
import io.fleetiq.proto.events.v1.DeviceProjectionEvent;
import io.fleetiq.proto.events.v1.PositionProjectionEvent;
import io.fleetiq.topology.domain.model.VehicleProjection;
import io.fleetiq.topology.domain.port.inbound.TopologyProjectionUseCase;
import io.fleetiq.topology.domain.port.outbound.ProjectionQuarantine;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.time.Instant;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Consumes the device and position events that build the topology read model.
 * Malformed messages are quarantined immediately; transient processing failures are retried
 * twice before quarantine. Successful completion therefore means either projection or durable
 * quarantine, allowing the broker message to be acknowledged safely.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class TopologyProjectionConsumer {

    private final TopologyProjectionUseCase useCase;
    private final ProjectionQuarantine quarantine;

    @Incoming("device-projections-in")
    public Uni<Void> consumeDevice(byte[] payload) {
        try {
            DeviceProjectionEvent event = DeviceProjectionEvent.parseFrom(payload);
            requireTenant(event.getTenantId());
            requireVin(event.getVin());
            return process("device-projection.v1", payload, () -> useCase.projectDevice(
                event.getTenantId(), new VehicleProjection(
                event.getVin(), event.getDeviceType(), event.getStatus(), null, null, null,
                Instant.ofEpochMilli(event.getOccurredAtEpochMillis()), null)));
        } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            return quarantine.quarantine("device-projection.v1", payload, e, 1);
        }
    }

    @Incoming("position-projections-in")
    public Uni<Void> consumePosition(byte[] payload) {
        try {
            PositionProjectionEvent event = PositionProjectionEvent.parseFrom(payload);
            requireTenant(event.getTenantId());
            requireVin(event.getVin());
            validateCoordinates(event.getLatitude(), event.getLongitude());
            return process("position-projection.v1", payload, () -> useCase.projectPosition(
                event.getTenantId(), event.getVin(), event.getLatitude(), event.getLongitude(),
                event.getAltitude(), Instant.ofEpochMilli(event.getObservedAtEpochMillis())));
        } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            return quarantine.quarantine("position-projection.v1", payload, e, 1);
        }
    }

    private Uni<Void> process(String eventType, byte[] payload, Supplier<Uni<Void>> action) {
        return Uni.createFrom().<Void>deferred(action::get)
            .onFailure().retry().withBackOff(Duration.ofMillis(50), Duration.ofSeconds(1)).atMost(2)
            .onFailure().recoverWithUni(failure ->
                quarantine.quarantine(eventType, payload, failure, 3));
    }

    private static void requireVin(String vin) {
        if (vin == null || vin.isBlank()) throw new IllegalArgumentException("VIN is required");
    }

    private static void requireTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("Tenant ID is required");
    }

    private static void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }
    }
}
