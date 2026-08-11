package io.fleetiq.simulator.mqtt;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MqttClientManager {

    private static final Logger log = LoggerFactory.getLogger(MqttClientManager.class);

    public void connect() {
        log.info("MQTT Client Manager initialized — will connect in Phase 1");
    }

    public void publishTelemetry(String vin, String jsonPayload) {
        log.debug("Publishing telemetry for {}: {}", vin, jsonPayload);
    }

    public void disconnect() {
        log.info("MQTT Client Manager disconnected");
    }
}
