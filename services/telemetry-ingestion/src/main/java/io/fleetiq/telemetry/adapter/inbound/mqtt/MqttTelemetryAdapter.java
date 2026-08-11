package io.fleetiq.telemetry.adapter.inbound.mqtt;

import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MqttTelemetryAdapter {

    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryAdapter.class);

    @Inject
    IngestTelemetryUseCase useCase;

    void onStart(@Observes StartupEvent ev) {
        log.info("MQTT Telemetry Adapter initialized");
        // Full MQTT subscriber registration in Phase 1
    }
}
