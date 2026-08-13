package io.fleetiq.streaming.domain.port.outbound;

import io.fleetiq.streaming.domain.model.PositionEvent;
import io.smallrye.mutiny.Multi;

/**
 * Source boundary for the live position stream. The returned stream remains lazy
 * and propagates downstream cancellation and backpressure to the adapter where possible.
 */
public interface PositionEventSource {

    /** Returns a live stream rather than a snapshot or in-memory subscriber registry. */
    Multi<PositionEvent> positions();
}
