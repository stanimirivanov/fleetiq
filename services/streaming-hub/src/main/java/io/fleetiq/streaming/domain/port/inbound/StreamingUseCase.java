package io.fleetiq.streaming.domain.port.inbound;

public interface StreamingUseCase {
    // Will be implemented with gRPC server streaming in Phase 1
    void registerSubscriber(String subscriberId);
    void unregisterSubscriber(String subscriberId);
}
