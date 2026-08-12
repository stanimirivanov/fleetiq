package io.fleetiq.telemetry.adapter;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.Map;

/**
 * Testcontainers resource that starts a Mosquitto MQTT broker.
 */
public class MqttTestResource implements QuarkusTestResourceLifecycleManager {

    private static final GenericContainer<?> MOSQUITTO = new GenericContainer<>(
        DockerImageName.parse("eclipse-mosquitto:2.0")
    )
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("mosquitto-test.conf"),
            "/mosquitto/config/mosquitto.conf"
        )
        .withExposedPorts(1883);

    @Override
    public Map<String, String> start() {
        MOSQUITTO.start();

        String host = MOSQUITTO.getHost();
        Integer port = MOSQUITTO.getMappedPort(1883);

        return Map.of(
            "mp.messaging.incoming.telemetry-in.enabled", "true",
            "mp.messaging.incoming.telemetry-in.host", host,
            "mp.messaging.incoming.telemetry-in.port", port.toString(),
            "mp.messaging.incoming.telemetry-in.client-id", "telemetry-ingestion-test"
        );
    }

    @Override
    public void stop() {
        MOSQUITTO.stop();
    }
}
