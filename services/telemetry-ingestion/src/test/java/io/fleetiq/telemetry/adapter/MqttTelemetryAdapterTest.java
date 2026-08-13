package io.fleetiq.telemetry.adapter;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

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
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            client.connect(options);
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish("fleetiq/tenant-a/" + vin + "/telemetry", message);
            client.disconnect();
        }

        awaitPersisted(vin, Duration.ofSeconds(10));
    }

    private void awaitPersisted(String vin, Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (countSamples(vin) == 1) {
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
