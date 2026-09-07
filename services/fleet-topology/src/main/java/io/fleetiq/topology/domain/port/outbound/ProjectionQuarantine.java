package io.fleetiq.topology.domain.port.outbound;

import io.smallrye.mutiny.Uni;

/** Stores projection messages that cannot be processed after a bounded number of attempts. */
public interface ProjectionQuarantine {
    /**
     * Preserves the original bytes and failure context for diagnosis or controlled replay.
     * Completing this operation acknowledges the poisoned source message.
     */
    Uni<Void> quarantine(String eventType, byte[] payload, Throwable failure, int attempts);
}
