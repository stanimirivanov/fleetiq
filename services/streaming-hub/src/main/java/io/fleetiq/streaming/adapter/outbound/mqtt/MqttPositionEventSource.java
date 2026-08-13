package io.fleetiq.streaming.adapter.outbound.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fleetiq.streaming.domain.model.PositionEvent;
import io.fleetiq.streaming.domain.port.outbound.PositionEventSource;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;

import java.time.Instant;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class MqttPositionEventSource implements PositionEventSource {

    @Channel("positions-in")
    Multi<MqttMessage<byte[]>> payloads;

    private final ObjectMapper objectMapper;

    @Override
    public Multi<PositionEvent> positions() {
        return payloads.onItem().transformToMultiAndConcatenate(message -> {
            try {
                return Multi.createFrom().item(toDomain(message.getTopic(), message.getPayload()));
            } catch (RuntimeException failure) {
                log.warn("Discarding invalid position event: {}", failure.getMessage());
                return Multi.createFrom().empty();
            }
        });
    }

    PositionEvent toDomain(String topic, byte[] payload) {
        try {
            TelemetryPayload event = objectMapper.readValue(payload, TelemetryPayload.class);
            if (event.vin() == null || event.vin().isBlank()) {
                throw new IllegalArgumentException("VIN is required");
            }
            String tenantId = tenantFromTopic(topic, event.vin());
            if (event.latitude() < -90 || event.latitude() > 90
                || event.longitude() < -180 || event.longitude() > 180) {
                throw new IllegalArgumentException("Invalid coordinates");
            }
            if (event.speedKmh() < 0) {
                throw new IllegalArgumentException("Speed cannot be negative");
            }
            return new PositionEvent(
                tenantId, event.vin(), Instant.parse(event.timestamp()), event.latitude(), event.longitude(),
                event.altitude(), event.speedKmh(), event.status());
        } catch (IllegalArgumentException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalArgumentException("Invalid telemetry JSON", failure);
        }
    }

    private static String tenantFromTopic(String topic, String vin) {
        String[] segments = topic == null ? new String[0] : topic.split("/");
        if (segments.length != 4 || !"fleetiq".equals(segments[0]) || segments[1].isBlank()
            || !vin.equals(segments[2]) || !"telemetry".equals(segments[3])) {
            throw new IllegalArgumentException("Tenant-qualified topic does not match payload VIN");
        }
        return segments[1];
    }

    record TelemetryPayload(
        String vin,
        String timestamp,
        double latitude,
        double longitude,
        double altitude,
        double speedKmh,
        String status
    ) {}
}
