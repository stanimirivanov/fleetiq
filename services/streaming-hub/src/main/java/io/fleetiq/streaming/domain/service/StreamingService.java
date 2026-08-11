package io.fleetiq.streaming.domain.service;

import io.fleetiq.streaming.domain.port.inbound.StreamingUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class StreamingService implements StreamingUseCase {

    private static final Logger log = LoggerFactory.getLogger(StreamingService.class);
    private final Set<String> subscribers = ConcurrentHashMap.newKeySet();

    @Override
    public void registerSubscriber(String subscriberId) {
        log.debug("Registering subscriber: {}", subscriberId);
        subscribers.add(subscriberId);
    }

    @Override
    public void unregisterSubscriber(String subscriberId) {
        log.debug("Unregistering subscriber: {}", subscriberId);
        subscribers.remove(subscriberId);
    }
}
