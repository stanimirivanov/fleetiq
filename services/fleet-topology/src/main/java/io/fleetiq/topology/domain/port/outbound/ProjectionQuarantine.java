package io.fleetiq.topology.domain.port.outbound;

import io.smallrye.mutiny.Uni;

/** Stores projection messages that cannot be processed after a bounded number of attempts. */
public interface ProjectionQuarantine {
    Uni<Void> quarantine(String eventType, byte[] payload, Throwable failure, int attempts);
}
