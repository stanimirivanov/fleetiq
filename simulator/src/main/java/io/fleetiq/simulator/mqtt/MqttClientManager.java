package io.fleetiq.simulator.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class MqttClientManager {

    private static final Logger log = LoggerFactory.getLogger(MqttClientManager.class);

    @Channel("telemetry-out")
    Emitter<byte[]> emitter;

    public void publishTelemetry(String vin, String jsonPayload) {
        String topic = "fleetiq/" + vin + "/telemetry";
        emitter.send(MqttMessage.of(
            topic,
            jsonPayload.getBytes(StandardCharsets.UTF_8),
            MqttQoS.AT_LEAST_ONCE
        ));
        log.debug("Published telemetry to {}", topic);
    }
}
