package io.fleetiq.telemetry.adapter.inbound.mqtt;

import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class MqttTelemetryAdapter {

    private final IngestTelemetryUseCase useCase;

    void onStart(@Observes StartupEvent ev) {
        log.info("MQTT Telemetry Adapter initialized — will subscribe to fleetiq/+/telemetry");
    }
}
