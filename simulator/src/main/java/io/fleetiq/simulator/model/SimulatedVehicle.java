package io.fleetiq.simulator.model;

/**
 * Mutable state used only by the local fleet simulator.
 * It is not a domain aggregate: its random walk exists to generate plausible changing input for
 * the production ingestion path.
 */
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

    /** Advances the vehicle by a small random offset and chooses a new road speed. */
    public void updatePosition() {
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
