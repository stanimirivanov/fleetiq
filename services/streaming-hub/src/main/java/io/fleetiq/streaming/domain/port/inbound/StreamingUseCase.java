package io.fleetiq.streaming.domain.port.inbound;

import io.fleetiq.streaming.domain.model.PositionEvent;
import io.smallrye.mutiny.Multi;

import java.time.Duration;
import java.util.Set;

/** Application boundary for live, tenant-isolated vehicle position subscriptions. */
public interface StreamingUseCase {
    /**
     * Watches selected VINs for one tenant. An empty VIN set means the entire tenant fleet, and
     * {@code minimumInterval} paces demand rather than blocking a worker thread.
     */
    Multi<PositionEvent> watchFleet(String tenantId, Set<String> vins, Duration minimumInterval);

    /** Watches one tenant-scoped vehicle until the subscriber cancels or the source terminates. */
    Multi<PositionEvent> watchVehicle(String tenantId, String vin, Duration minimumInterval);
}
