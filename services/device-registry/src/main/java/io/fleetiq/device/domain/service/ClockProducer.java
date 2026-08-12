package io.fleetiq.device.domain.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Clock;

@ApplicationScoped
public class ClockProducer {
    @Produces
    public Clock clock() {
        return Clock.systemUTC();
    }
}
