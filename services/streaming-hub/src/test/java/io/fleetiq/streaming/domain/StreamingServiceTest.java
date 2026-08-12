package io.fleetiq.streaming.domain;

import io.fleetiq.streaming.domain.model.PositionEvent;
import io.fleetiq.streaming.domain.port.outbound.PositionEventSource;
import io.fleetiq.streaming.domain.service.StreamingService;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamingServiceTest {

    private final PositionEvent first = event("1HGCM82633A004352");
    private final PositionEvent second = event("JH4KA4650MC000000");
    private final PositionEventSource source = () -> Multi.createFrom().items(first, second);
    private final StreamingService service = new StreamingService(source);

    @Test
    void filtersFleetByRequestedVins() {
        List<PositionEvent> events = service.watchFleet(
                Set.of(second.vin()), Duration.ZERO)
            .collect().asList().await().indefinitely();

        assertEquals(List.of(second), events);
    }

    @Test
    void filtersSingleVehicleFromSharedSource() {
        List<PositionEvent> events = service.watchVehicle(
                first.vin(), Duration.ZERO)
            .collect().asList().await().indefinitely();

        assertEquals(List.of(first), events);
    }

    private static PositionEvent event(String vin) {
        return new PositionEvent(
            vin, Instant.parse("2026-08-12T12:00:00Z"), 52.52, 13.405, 34, 72.5, "MOVING");
    }
}
