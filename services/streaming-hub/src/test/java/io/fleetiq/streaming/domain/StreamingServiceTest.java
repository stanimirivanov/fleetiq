package io.fleetiq.streaming.domain;

import io.fleetiq.streaming.domain.model.PositionEvent;
import io.fleetiq.streaming.domain.port.outbound.PositionEventSource;
import io.fleetiq.streaming.domain.service.StreamingService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamingServiceTest {

    private final PositionEvent first = event("tenant-a", "1HGCM82633A004352");
    private final PositionEvent second = event("tenant-a", "JH4KA4650MC000000");
    private final PositionEvent otherTenant = event("tenant-b", first.vin());
    private final PositionEventSource source = () -> Multi.createFrom().items(first, second, otherTenant);
    private final StreamingService service = new StreamingService(source, 256);

    @Test
    void filtersFleetByRequestedVins() {
        List<PositionEvent> events = service.watchFleet(
                "tenant-a", Set.of(second.vin()), Duration.ZERO)
            .collect().asList().await().indefinitely();

        assertEquals(List.of(second), events);
    }

    @Test
    void filtersSingleVehicleFromSharedSource() {
        List<PositionEvent> events = service.watchVehicle(
                "tenant-a", first.vin(), Duration.ZERO)
            .collect().asList().await().indefinitely();

        assertEquals(List.of(first), events);
    }

    @Test
    void neverEmitsAnotherTenantsIdenticalVin() {
        List<PositionEvent> events = service.watchVehicle(
                "tenant-a", first.vin(), Duration.ZERO)
            .collect().asList().await().indefinitely();

        assertEquals(List.of(first), events);
    }

    @Test
    void failsAClientThatExceedsItsBoundedSlowConsumerBuffer() {
        AtomicReference<MultiEmitter<? super PositionEvent>> emitter = new AtomicReference<>();
        PositionEventSource hotSource = () -> Multi.createFrom().<PositionEvent>emitter(
            value -> emitter.set(value), BackPressureStrategy.IGNORE);
        var boundedService = new StreamingService(hotSource, 2);
        var subscriber = AssertSubscriber.<PositionEvent>create(0);

        boundedService.watchVehicle("tenant-a", first.vin(), Duration.ZERO)
            .subscribe().withSubscriber(subscriber);
        emitter.get().emit(first).emit(first).emit(first);

        subscriber.awaitFailure().assertFailedWith(RuntimeException.class);
    }

    private static PositionEvent event(String tenantId, String vin) {
        return new PositionEvent(
            tenantId, vin, Instant.parse("2026-08-12T12:00:00Z"), 52.52, 13.405, 34, 72.5, "MOVING");
    }
}
