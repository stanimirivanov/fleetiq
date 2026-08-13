package io.fleetiq.telemetry.adapter.inbound.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class MqttTelemetryAdapter {

    private final IngestTelemetryUseCase useCase;
    private final ObjectMapper objectMapper;

    @Incoming("telemetry-in")
    public Uni<Void> consume(MqttMessage<byte[]> message) {
        return Uni.createFrom().item(() -> toEnvelope(message))
            .onItem().transformToUni(envelope -> useCase.ingest(envelope.tenantId(), envelope.sample()))
            .onItem().transformToUni(result -> result.accepted()
                ? Uni.createFrom().voidItem()
                : Uni.createFrom().failure(new IllegalStateException(result.message())))
            .onFailure().invoke(error ->
                log.error("Failed to ingest MQTT telemetry from {}", message.getTopic(), error));
    }

    TelemetrySample toDomain(MqttMessage<byte[]> message) {
        return toEnvelope(message).sample();
    }

    private TelemetryEnvelope toEnvelope(MqttMessage<byte[]> message) {
        try {
            TelemetryPayload payload = objectMapper.readValue(message.getPayload(), TelemetryPayload.class);
            String tenantId = validate(message.getTopic(), payload);
            return new TelemetryEnvelope(tenantId, new TelemetrySample(
                payload.vin(), Instant.parse(payload.timestamp()),
                payload.latitude(), payload.longitude(), payload.altitude(),
                payload.speedKmh(), payload.fuelLevelPct(), payload.engineTempCelsius(),
                payload.batteryVoltage(), payload.customMetrics() == null ? Map.of() : payload.customMetrics()
            ));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Telemetry timestamp must be ISO-8601", e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid telemetry JSON", e);
        }
    }

    private static String validate(String topic, TelemetryPayload payload) {
        if (payload.vin() == null || payload.vin().isBlank()) {
            throw new IllegalArgumentException("VIN is required");
        }
        String[] segments = topic.split("/");
        if (segments.length != 4 || !"fleetiq".equals(segments[0])
            || segments[1].isBlank() || !payload.vin().equals(segments[2])
            || !"telemetry".equals(segments[3])) {
            throw new IllegalArgumentException("Topic VIN does not match payload VIN");
        }
        if (payload.latitude() < -90 || payload.latitude() > 90
            || payload.longitude() < -180 || payload.longitude() > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }
        if (payload.speedKmh() < 0 || payload.fuelLevelPct() < 0 || payload.fuelLevelPct() > 100) {
            throw new IllegalArgumentException("Invalid telemetry measurement");
        }
        return segments[1];
    }

    private record TelemetryEnvelope(String tenantId, TelemetrySample sample) {}

    record TelemetryPayload(
        String vin,
        String timestamp,
        double latitude,
        double longitude,
        double altitude,
        double speedKmh,
        double fuelLevelPct,
        double engineTempCelsius,
        double batteryVoltage,
        Map<String, Double> customMetrics
    ) {}
}
