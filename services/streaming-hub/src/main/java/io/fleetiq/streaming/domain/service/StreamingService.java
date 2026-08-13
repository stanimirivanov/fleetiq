package io.fleetiq.streaming.domain.service;

import io.fleetiq.streaming.domain.model.PositionEvent;
import io.fleetiq.streaming.domain.port.inbound.StreamingUseCase;
import io.fleetiq.streaming.domain.port.outbound.PositionEventSource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.FixedDemandPacer;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
@RequiredArgsConstructor
public class StreamingService implements StreamingUseCase {

    private final PositionEventSource eventSource;

    @Override
    public Multi<PositionEvent> watchFleet(String tenantId, Set<String> vins, Duration minimumInterval) {
        if (tenantId == null || tenantId.isBlank()) {
            return Multi.createFrom().failure(new IllegalArgumentException("Tenant ID is required"));
        }
        Set<String> selectedVins = vins == null ? Set.of() : Set.copyOf(vins);
        Multi<PositionEvent> stream = eventSource.positions()
            .select().where(event -> tenantId.equals(event.tenantId())
                && (selectedVins.isEmpty() || selectedVins.contains(event.vin())));
        return throttle(stream, minimumInterval);
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
        return throttle(stream, minimumInterval);
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
}
