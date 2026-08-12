package io.fleetiq.streaming.domain.port.outbound;

import io.fleetiq.streaming.domain.model.PositionEvent;
import io.smallrye.mutiny.Multi;

public interface PositionEventSource {
    Multi<PositionEvent> positions();
}
