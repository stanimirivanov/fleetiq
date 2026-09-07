package io.fleetiq.streaming.domain.service;

import io.fleetiq.streaming.domain.model.PositionEvent;
import io.fleetiq.streaming.domain.port.inbound.StreamingUseCase;
import io.fleetiq.streaming.domain.port.outbound.PositionEventSource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.FixedDemandPacer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Set;

/**
 * Builds filtered, demand-paced streams from the shared position event source.
 * Tenant filtering is always applied before optional VIN filtering, and each subscriber receives
 * a bounded overflow buffer so a slow client cannot consume unlimited memory.
 */
@ApplicationScoped
public class StreamingService implements StreamingUseCase {

    private final PositionEventSource eventSource;
    private final int maxBufferedEvents;

    @Inject
    public StreamingService(PositionEventSource eventSource,
        @ConfigProperty(name = "fleetiq.streaming.max-buffered-events", defaultValue = "256")
        int maxBufferedEvents) {
        if (maxBufferedEvents < 1 || maxBufferedEvents > 10_000) {
            throw new IllegalArgumentException("Buffered event limit must be between 1 and 10000");
        }
        this.eventSource = eventSource;
        this.maxBufferedEvents = maxBufferedEvents;
    }

    @Override
    public Multi<PositionEvent> watchFleet(String tenantId, Set<String> vins, Duration minimumInterval) {
        if (tenantId == null || tenantId.isBlank()) {
            return Multi.createFrom().failure(new IllegalArgumentException("Tenant ID is required"));
        }
        Set<String> selectedVins = vins == null ? Set.of() : Set.copyOf(vins);
        Multi<PositionEvent> stream = eventSource.positions()
            .select().where(event -> tenantId.equals(event.tenantId())
                && (selectedVins.isEmpty() || selectedVins.contains(event.vin())));
        return bounded(throttle(stream, minimumInterval));
    }

    @Override
    public Multi<PositionEvent> watchVehicle(String tenantId, String vin, Duration minimumInterval) {
        if (tenantId == null || tenantId.isBlank()) {
            return Multi.createFrom().failure(new IllegalArgumentException("Tenant ID is required"));
        }
        if (vin == null || vin.isBlank()) {
            return Multi.createFrom().failure(new IllegalArgumentException("VIN is required"));
        }
        Multi<PositionEvent> stream = eventSource.positions()
            .select().where(event -> tenantId.equals(event.tenantId()) && vin.equals(event.vin()));
        return bounded(throttle(stream, minimumInterval));
    }

    private Multi<PositionEvent> throttle(Multi<PositionEvent> stream, Duration minimumInterval) {
        if (minimumInterval == null || minimumInterval.isZero()) {
            return stream;
        }
        if (minimumInterval.isNegative()) {
            return Multi.createFrom().failure(
                new IllegalArgumentException("Minimum update interval cannot be negative"));
        }
        return stream.paceDemand().using(new FixedDemandPacer(1, minimumInterval));
    }

    private Multi<PositionEvent> bounded(Multi<PositionEvent> stream) {
        return stream.onOverflow().buffer(maxBufferedEvents);
    }
}
