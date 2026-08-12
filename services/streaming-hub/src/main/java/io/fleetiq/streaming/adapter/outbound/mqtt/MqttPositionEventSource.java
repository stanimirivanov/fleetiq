package io.fleetiq.streaming.adapter.outbound.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fleetiq.streaming.domain.model.PositionEvent;
import io.fleetiq.streaming.domain.port.outbound.PositionEventSource;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;

import java.time.Instant;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class MqttPositionEventSource implements PositionEventSource {

    @Channel("positions-in")
    Multi<byte[]> payloads;

    private final ObjectMapper objectMapper;

    @Override
    public Multi<PositionEvent> positions() {
        return payloads.onItem().transformToMultiAndConcatenate(payload -> {
            try {
                return Multi.createFrom().item(toDomain(payload));
            } catch (RuntimeException failure) {
                log.warn("Discarding invalid position event: {}", failure.getMessage());
                return Multi.createFrom().empty();
            }
        });
    }

    PositionEvent toDomain(byte[] payload) {
        try {
            TelemetryPayload event = objectMapper.readValue(payload, TelemetryPayload.class);
            if (event.vin() == null || event.vin().isBlank()) {
                throw new IllegalArgumentException("VIN is required");
            }
            if (event.latitude() < -90 || event.latitude() > 90
                || event.longitude() < -180 || event.longitude() > 180) {
                throw new IllegalArgumentException("Invalid coordinates");
            }
            if (event.speedKmh() < 0) {
                throw new IllegalArgumentException("Speed cannot be negative");
            }
            return new PositionEvent(
                event.vin(), Instant.parse(event.timestamp()), event.latitude(), event.longitude(),
                event.altitude(), event.speedKmh(), event.status());
        } catch (IllegalArgumentException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalArgumentException("Invalid telemetry JSON", failure);
        }
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
