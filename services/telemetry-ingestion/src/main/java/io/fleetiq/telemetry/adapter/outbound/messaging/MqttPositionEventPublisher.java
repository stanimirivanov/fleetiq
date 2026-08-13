package io.fleetiq.telemetry.adapter.outbound.messaging;

import io.fleetiq.proto.events.v1.PositionProjectionEvent;
import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.fleetiq.telemetry.domain.port.outbound.PositionEventPublisher;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;

@ApplicationScoped
public class MqttPositionEventPublisher implements PositionEventPublisher {

    private final MutinyEmitter<byte[]> emitter;

    public MqttPositionEventPublisher(@Channel("position-projections-out") MutinyEmitter<byte[]> emitter) {
        this.emitter = emitter;
    }

    @Override
    public Uni<Void> publish(TelemetrySample sample) {
        byte[] payload = PositionProjectionEvent.newBuilder()
            .setVin(sample.vin())
            .setLatitude(sample.latitude())
            .setLongitude(sample.longitude())
            .setAltitude(sample.altitude())
            .setObservedAtEpochMillis(sample.timestamp().toEpochMilli())
            .build().toByteArray();
        return emitter.send(payload);
    }
}
