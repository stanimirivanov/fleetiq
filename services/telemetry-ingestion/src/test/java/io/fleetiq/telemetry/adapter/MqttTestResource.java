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
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("mosquitto-test-passwords", 0600),
            "/mosquitto/runtime/passwords"
        )
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("mosquitto-test.acl", 0600),
            "/mosquitto/runtime/acl"
        )
        .withExposedPorts(1883);

    @Override
    public Map<String, String> start() {
        MOSQUITTO.start();

        String host = MOSQUITTO.getHost();
        Integer port = MOSQUITTO.getMappedPort(1883);

        return Map.ofEntries(
            Map.entry("mp.messaging.incoming.telemetry-in.enabled", "true"),
            Map.entry("mp.messaging.incoming.telemetry-in.host", host),
            Map.entry("mp.messaging.incoming.telemetry-in.port", port.toString()),
            Map.entry("mp.messaging.incoming.telemetry-in.client-id", "telemetry-ingestion-test"),
            Map.entry("mp.messaging.incoming.telemetry-in.username", "telemetry-ingestion"),
            Map.entry("mp.messaging.incoming.telemetry-in.password", "telemetry-ingestion-test"),
            Map.entry("mp.messaging.outgoing.position-projections-out.enabled", "true"),
            Map.entry("mp.messaging.outgoing.position-projections-out.host", host),
            Map.entry("mp.messaging.outgoing.position-projections-out.port", port.toString()),
            Map.entry("mp.messaging.outgoing.position-projections-out.username", "telemetry-ingestion"),
            Map.entry("mp.messaging.outgoing.position-projections-out.password", "telemetry-ingestion-test")
        );
    }

    @Override
    public void stop() {
        MOSQUITTO.stop();
    }
}
