package io.fleetiq.pekko.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VehicleStateServiceContractTest {

    @Test
    void validatesTelemetryAtTheServiceBoundary() {
        assertThrows(IllegalArgumentException.class, () ->
            new VehicleStateService.TelemetryUpdate(
                "invalid", Instant.now(), 52.52, 13.405, 10));
        assertThrows(IllegalArgumentException.class, () ->
            new VehicleStateService.TelemetryUpdate(
                "1HGCM82633A004352", Instant.now(), 91, 13.405, 10));
        assertThrows(IllegalArgumentException.class, () ->
            new VehicleStateService.TelemetryUpdate(
                "1HGCM82633A004352", Instant.now(), 52.52, 13.405, -1));
    }

    @Test
    void normalizesNullCommandPayload() {
        var command = new VehicleStateService.VehicleCommand(
            "1HGCM82633A004352", "LOCK", null);

        assertEquals("", command.payload());
    }
}
