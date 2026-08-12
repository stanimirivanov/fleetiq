package io.fleetiq.telemetry.adapter.inbound.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqttTelemetryMappingTest {

    @Test
    void mapsAndDelegatesValidTelemetry() {
        CapturingUseCase useCase = new CapturingUseCase();
        MqttTelemetryAdapter adapter = new MqttTelemetryAdapter(useCase, new ObjectMapper());

        adapter.consume(message("VIN-123", "VIN-123")).await().indefinitely();

        assertEquals("VIN-123", useCase.sample.vin());
        assertEquals(52.52, useCase.sample.latitude());
        assertEquals(3.4, useCase.sample.customMetrics().get("oil_pressure"));
    }

    @Test
    void rejectsTopicAndPayloadVinMismatch() {
        MqttTelemetryAdapter adapter = new MqttTelemetryAdapter(new CapturingUseCase(), new ObjectMapper());

        assertThrows(IllegalArgumentException.class,
            () -> adapter.consume(message("VIN-123", "VIN-OTHER")).await().indefinitely());
    }

    private static MqttMessage<byte[]> message(String topicVin, String payloadVin) {
        String json = """
            {"vin":"%s","timestamp":"2026-08-12T20:00:00Z",
             "latitude":52.52,"longitude":13.405,"altitude":34.0,
             "speedKmh":72.5,"fuelLevelPct":68.0,"engineTempCelsius":91.2,
             "batteryVoltage":12.7,"customMetrics":{"oil_pressure":3.4}}
            """.formatted(payloadVin);
        return MqttMessage.of(
            "fleetiq/" + topicVin + "/telemetry",
            json.getBytes(StandardCharsets.UTF_8),
            MqttQoS.AT_LEAST_ONCE
        );
    }

    private static final class CapturingUseCase implements IngestTelemetryUseCase {
        private TelemetrySample sample;

        @Override
        public Uni<IngestResult> ingest(TelemetrySample sample) {
            this.sample = sample;
            return Uni.createFrom().item(new IngestResult(true, "Telemetry accepted"));
        }

        @Override
        public Uni<List<TelemetrySample>> getTelemetryRange(String vin, Instant from, Instant to) {
            return Uni.createFrom().item(List.of());
        }

        @Override
        public Uni<Double> getAverageSpeed(String vin, Instant from, Instant to) {
            return Uni.createFrom().item(0.0);
        }
    }
}
