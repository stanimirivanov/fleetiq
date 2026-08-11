package io.fleetiq.simulator.mqtt;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class MqttClientManager {

    private static final Logger log = LoggerFactory.getLogger(MqttClientManager.class);

    @ConfigProperty(name = "mqtt.broker.host", defaultValue = "localhost")
    String brokerHost;

    @ConfigProperty(name = "mqtt.broker.port", defaultValue = "1883")
    int brokerPort;

    private final ConcurrentMap<String, Boolean> connectedVehicles = new ConcurrentHashMap<>();

    void onStart(@Observes StartupEvent ev) {
        log.info("MQTT Client Manager initialized — broker: {}:{}", brokerHost, brokerPort);
    }

    public void publishTelemetry(String vin, String jsonPayload) {
        log.info("[MQTT] {} -> fleetiq/{}/telemetry", vin, vin);
        log.debug("[MQTT] Payload: {}", jsonPayload);
        connectedVehicles.putIfAbsent(vin, true);
    }

    public void publishCommandResponse(String vin, String responseJson) {
        log.info("[MQTT] {} -> fleetiq/{}/command/response", vin, vin);
    }

    public boolean isConnected(String vin) {
        return connectedVehicles.getOrDefault(vin, false);
    }
}
