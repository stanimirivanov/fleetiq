package io.fleetiq.simulator.model;

public class SimulatedVehicle {

    private final String vin;
    private final String deviceType;
    private double latitude;
    private double longitude;
    private double speedKmh;

    public SimulatedVehicle(String vin, String deviceType, double startLat, double startLon) {
        this.vin = vin;
        this.deviceType = deviceType;
        this.latitude = startLat;
        this.longitude = startLon;
        this.speedKmh = 0.0;
    }

    public void updatePosition() {
        // Simulate movement — will be implemented in Phase 1
        this.latitude += (Math.random() - 0.5) * 0.001;
        this.longitude += (Math.random() - 0.5) * 0.001;
        this.speedKmh = 30 + Math.random() * 80;
    }

    public String getVin() { return vin; }
    public String getDeviceType() { return deviceType; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getSpeedKmh() { return speedKmh; }
}
