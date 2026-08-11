package io.fleetiq.simulator;

import io.fleetiq.simulator.model.SimulatedVehicle;
import io.fleetiq.simulator.mqtt.MqttClientManager;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class VehicleSimulator {

    private static final Logger log = LoggerFactory.getLogger(VehicleSimulator.class);
    private final List<SimulatedVehicle> vehicles = new ArrayList<>();

    @Inject
    MqttClientManager mqttManager;

    void onStart(@Observes StartupEvent ev) {
        log.info("Vehicle Simulator starting...");
        createSimulatedVehicles();
        mqttManager.connect();
        log.info("Created {} simulated vehicles", vehicles.size());
    }

    private void createSimulatedVehicles() {
        // Create 5 simulated vehicles in different cities
        vehicles.add(new SimulatedVehicle("SIM-VIN-001", "TRUCK", 52.5200, 13.4050));   // Berlin
        vehicles.add(new SimulatedVehicle("SIM-VIN-002", "TRUCK", 48.8566, 2.3522));    // Paris
        vehicles.add(new SimulatedVehicle("SIM-VIN-003", "CAR", 51.5074, -0.1278));     // London
        vehicles.add(new SimulatedVehicle("SIM-VIN-004", "VAN", 40.7128, -74.0060));    // New York
        vehicles.add(new SimulatedVehicle("SIM-VIN-005", "TRUCK", 35.6762, 139.6503));  // Tokyo
    }

    public List<SimulatedVehicle> getVehicles() {
        return vehicles;
    }
}
