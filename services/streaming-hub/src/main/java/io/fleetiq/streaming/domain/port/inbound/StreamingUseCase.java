package io.fleetiq.streaming.domain.port.inbound;

import io.fleetiq.streaming.domain.model.PositionEvent;
import io.smallrye.mutiny.Multi;

import java.time.Duration;
import java.util.Set;

public interface StreamingUseCase {
    Multi<PositionEvent> watchFleet(Set<String> vins, Duration minimumInterval);
    Multi<PositionEvent> watchVehicle(String vin, Duration minimumInterval);
}
