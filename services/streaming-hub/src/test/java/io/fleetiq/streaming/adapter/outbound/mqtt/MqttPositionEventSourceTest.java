package io.fleetiq.streaming.adapter.outbound.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqttPositionEventSourceTest {

    private final MqttPositionEventSource source = new MqttPositionEventSource(new ObjectMapper());

    @Test
    void mapsSimulatorTelemetryToPositionEvent() {
        var event = source.toDomain(json("52.52", "13.405"));

        assertEquals("1HGCM82633A004352", event.vin());
        assertEquals(Instant.parse("2026-08-12T12:00:00Z"), event.observedAt());
        assertEquals(72.5, event.speedKmh());
    }

    @Test
    void rejectsInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> source.toDomain(json("91", "13.405")));
    }

    private static byte[] json(String latitude, String longitude) {
        return ("""
            {"vin":"1HGCM82633A004352","timestamp":"2026-08-12T12:00:00Z",
             "latitude":%s,"longitude":%s,"altitude":34,"speedKmh":72.5}
            """).formatted(latitude, longitude).getBytes(StandardCharsets.UTF_8);
    }
}
