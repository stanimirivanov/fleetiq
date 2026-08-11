package io.fleetiq.telemetry.adapter;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * Testcontainers resource that starts a Mosquitto MQTT broker.
 */
public class MqttTestResource implements QuarkusTestResourceLifecycleManager {

    private static final GenericContainer<?> MOSQUITTO = new GenericContainer<>(
        DockerImageName.parse("eclipse-mosquitto:2.0")
    )
        .withExposedPorts(1883);

    @Override
    public Map<String, String> start() {
        MOSQUITTO.start();

        String host = MOSQUITTO.getHost();
        Integer port = MOSQUITTO.getMappedPort(1883);

        return Map.of(
            "quarkus.messaging.mqtt.telemetry-ingest.host", host,
            "quarkus.messaging.mqtt.telemetry-ingest.port", port.toString(),
            "quarkus.messaging.mqtt.telemetry-ingest.auto-generated-client-id", "false",
            "quarkus.messaging.mqtt.telemetry-ingest.client-id", "test-client"
        );
    }

    @Override
    public void stop() {
        MOSQUITTO.stop();
    }
}
