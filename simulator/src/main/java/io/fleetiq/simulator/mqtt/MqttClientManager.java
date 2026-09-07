package io.fleetiq.simulator.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;

/**
 * Outbound MQTT adapter for simulated telemetry.
 * It constructs the tenant-qualified topic expected by ingestion and requests QoS 1 delivery,
 * so the downstream path must tolerate duplicate messages.
 */
@ApplicationScoped
public class MqttClientManager {

    private static final Logger log = LoggerFactory.getLogger(MqttClientManager.class);

    @Channel("telemetry-out")
    Emitter<byte[]> emitter;

    @ConfigProperty(name = "fleetiq.simulator.tenant-id", defaultValue = "demo")
    String tenantId;

    /**
     * Publishes one JSON telemetry document under the configured simulator tenant.
     * Publication completion is managed by the reactive messaging connector and its configured
     * failure strategy.
     */
    public void publishTelemetry(String vin, String jsonPayload) {
        String topic = "fleetiq/" + tenantId + "/" + vin + "/telemetry";
        emitter.send(MqttMessage.of(
            topic,
            jsonPayload.getBytes(StandardCharsets.UTF_8),
            MqttQoS.AT_LEAST_ONCE
        ));
        log.debug("Published telemetry to {}", topic);
    }
}
