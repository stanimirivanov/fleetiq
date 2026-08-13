package io.fleetiq.telemetry.adapter;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
@QuarkusTestResource(MqttTestResource.class)
class MqttTelemetryAdapterTest {

    @ConfigProperty(name = "mp.messaging.incoming.telemetry-in.host")
    String mqttHost;

    @ConfigProperty(name = "mp.messaging.incoming.telemetry-in.port")
    int mqttPort;

    @Inject
    DataSource dataSource;

    @Test
    void persistsPublishedTelemetry() throws Exception {
        String vin = "VIN-MQTT-00000001";
        String payload = """
            {
              "vin": "%s",
              "timestamp": "%s",
              "latitude": 52.52,
              "longitude": 13.405,
              "altitude": 34.0,
              "speedKmh": 72.5,
              "fuelLevelPct": 68.0,
              "engineTempCelsius": 91.2,
              "batteryVoltage": 12.7,
              "customMetrics": {"oil_pressure": 3.4}
            }
            """.formatted(vin, Instant.now());

        String brokerUri = "tcp://" + mqttHost + ":" + mqttPort;
        try (MqttClient client = new MqttClient(brokerUri, MqttClient.generateClientId())) {
            MqttConnectOptions options = authenticatedOptions("demo", "simulator-demo-test");
            client.connect(options);
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish("fleetiq/demo/" + vin + "/telemetry", message);
            client.disconnect();
        }

        awaitPersisted(vin, Duration.ofSeconds(10));
    }

    @Test
    void rejectsAnonymousConnections() throws Exception {
        try (MqttClient client = new MqttClient(brokerUri(), MqttClient.generateClientId())) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            assertThrows(MqttException.class, () -> client.connect(options));
        }
    }

    @Test
    void preventsSimulatorFromPublishingIntoAnotherTenant() throws Exception {
        String vin = "VIN-MQTT-DENIED01";

        try (MqttClient client = new MqttClient(brokerUri(), MqttClient.generateClientId())) {
            client.connect(authenticatedOptions("demo", "simulator-demo-test"));
            try {
                client.publish(
                    "fleetiq/another-tenant/" + vin + "/telemetry",
                    telemetryMessage(vin)
                );
            } catch (Exception ignored) {
                // MQTT 3 brokers may report an ACL denial by closing the connection.
            }
            disconnectIfConnected(client);
        }

        Thread.sleep(500);
        assertEquals(0, countSamples(vin), "Cross-tenant telemetry must not reach ingestion");
    }

    @Test
    void bindsDeviceIdentityToItsExactTenantAndVinTopic() throws Exception {
        String ownVin = "SIM-VIN-001";
        String otherVin = "SIM-VIN-002";

        try (MqttClient client = new MqttClient(brokerUri(), MqttClient.generateClientId())) {
            client.connect(authenticatedOptions("demo/SIM-VIN-001", "device-001-test"));
            client.publish("fleetiq/demo/" + ownVin + "/telemetry", telemetryMessage(ownVin));
            try {
                client.publish("fleetiq/demo/" + otherVin + "/telemetry", telemetryMessage(otherVin));
            } catch (MqttException ignored) {
                // MQTT 3 brokers may report an ACL denial by closing the connection.
            }
            disconnectIfConnected(client);
        }

        awaitPersisted(ownVin, Duration.ofSeconds(10));
        Thread.sleep(500);
        assertEquals(0, countSamples(otherVin), "A device identity must not publish for another VIN");
    }

    @Test
    void restrictsBackendIdentityToItsProjectionTopic() throws Exception {
        var receivedTopics = new LinkedBlockingQueue<String>();

        try (MqttClient subscriber = new MqttClient(brokerUri(), MqttClient.generateClientId());
             MqttClient publisher = new MqttClient(brokerUri(), MqttClient.generateClientId())) {
            subscriber.connect(authenticatedOptions("fleet-topology", "fleet-topology-test"));
            subscriber.subscribe(
                "fleetiq/events/device-projections",
                1,
                (topic, message) -> receivedTopics.offer(topic)
            );
            subscriber.subscribe(
                "fleetiq/events/position-projections",
                1,
                (topic, message) -> receivedTopics.offer(topic)
            );

            publisher.connect(authenticatedOptions("device-registry", "device-registry-test"));
            publisher.publish(
                "fleetiq/events/device-projections",
                new MqttMessage("allowed".getBytes(StandardCharsets.UTF_8))
            );
            assertEquals(
                "fleetiq/events/device-projections",
                receivedTopics.poll(2, TimeUnit.SECONDS)
            );

            try {
                publisher.publish(
                    "fleetiq/events/position-projections",
                    new MqttMessage("denied".getBytes(StandardCharsets.UTF_8))
                );
            } catch (Exception ignored) {
                // MQTT 3 brokers may report an ACL denial by closing the connection.
            }
            assertNull(
                receivedTopics.poll(500, TimeUnit.MILLISECONDS),
                "Device registry must not publish position projections"
            );
            disconnectIfConnected(publisher);
            disconnectIfConnected(subscriber);
        }
    }

    private void disconnectIfConnected(MqttClient client) throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    private MqttConnectOptions authenticatedOptions(String username, String password) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);
        options.setCleanSession(true);
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        return options;
    }

    private MqttMessage telemetryMessage(String vin) {
        String payload = """
            {"vin":"%s","timestamp":"%s","latitude":52.52,"longitude":13.405,
             "speedKmh":72.5,"fuelLevelPct":68.0,"engineTempCelsius":91.2,
             "batteryVoltage":12.7,"customMetrics":{}}
            """.formatted(vin, Instant.now());
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        return message;
    }

    private String brokerUri() {
        return "tcp://" + mqttHost + ":" + mqttPort;
    }

    private void awaitPersisted(String vin, Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (countSamples(vin) >= 1) {
                return;
            }
            Thread.sleep(100);
        }
        fail("Telemetry was not persisted before timeout");
    }

    private int countSamples(String vin) throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                 "SELECT count(*) FROM telemetry_samples WHERE vin = ?")) {
            statement.setString(1, vin);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
}
