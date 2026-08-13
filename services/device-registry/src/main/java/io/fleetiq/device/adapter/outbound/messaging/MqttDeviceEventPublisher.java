package io.fleetiq.device.adapter.outbound.messaging;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.port.outbound.DeviceEventPublisher;
import io.fleetiq.proto.events.v1.DeviceProjectionEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;

import java.time.Instant;
import java.time.Clock;

@ApplicationScoped
public class MqttDeviceEventPublisher implements DeviceEventPublisher {

    private final MutinyEmitter<byte[]> emitter;
    private final Clock clock;

    public MqttDeviceEventPublisher(@Channel("device-projections-out") MutinyEmitter<byte[]> emitter, Clock clock) {
        this.emitter = emitter;
        this.clock = clock;
    }

    @Override
    public Uni<Void> publish(Device device) {
        Instant occurredAt = device.updatedAt() != null ? device.updatedAt()
            : device.registeredAt() != null ? device.registeredAt() : clock.instant();
        byte[] payload = DeviceProjectionEvent.newBuilder()
            .setVin(device.vin())
            .setDeviceType(device.deviceType())
            .setStatus(device.status().name())
            .setOccurredAtEpochMillis(occurredAt.toEpochMilli())
            .build().toByteArray();
        return emitter.send(payload);
    }
}
