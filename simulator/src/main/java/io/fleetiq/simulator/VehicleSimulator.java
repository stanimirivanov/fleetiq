package io.fleetiq.simulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fleetiq.simulator.model.SimulatedVehicle;
import io.fleetiq.simulator.mqtt.MqttClientManager;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class VehicleSimulator {

    private static final Logger log = LoggerFactory.getLogger(VehicleSimulator.class);
    private final List<SimulatedVehicle> vehicles = new ArrayList<>();
    @Inject
    MqttClientManager mqttManager;

    @Inject
    ObjectMapper objectMapper;

    void onStart(@Observes StartupEvent ev) {
        log.info("Vehicle Simulator starting...");
        createSimulatedVehicles();
        log.info("Created {} simulated vehicles", vehicles.size());
    }

    private void createSimulatedVehicles() {
        vehicles.add(new SimulatedVehicle("SIM-VIN-001", "TRUCK", 52.5200, 13.4050));
        vehicles.add(new SimulatedVehicle("SIM-VIN-002", "TRUCK", 48.8566, 2.3522));
        vehicles.add(new SimulatedVehicle("SIM-VIN-003", "CAR", 51.5074, -0.1278));
        vehicles.add(new SimulatedVehicle("SIM-VIN-004", "VAN", 40.7128, -74.0060));
        vehicles.add(new SimulatedVehicle("SIM-VIN-005", "TRUCK", 35.6762, 139.6503));
    }

    @Scheduled(every = "2s")
    void publishTelemetry() {
        for (SimulatedVehicle vehicle : vehicles) {
            vehicle.updatePosition();
            try {
                Map<String, Object> telemetry = Map.of(
                    "vin", vehicle.getVin(),
                    "timestamp", Instant.now().toString(),
                    "latitude", vehicle.getLatitude(),
                    "longitude", vehicle.getLongitude(),
                    "speedKmh", vehicle.getSpeedKmh(),
                    "fuelLevelPct", 50 + Math.random() * 50,
                    "engineTempCelsius", 80 + Math.random() * 20,
                    "batteryVoltage", 12.0 + Math.random() * 2
                );
                String json = objectMapper.writeValueAsString(telemetry);
                mqttManager.publishTelemetry(vehicle.getVin(), json);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize telemetry for {}", vehicle.getVin(), e);
            }
        }
    }

    public List<SimulatedVehicle> getVehicles() {
        return vehicles;
    }
}
