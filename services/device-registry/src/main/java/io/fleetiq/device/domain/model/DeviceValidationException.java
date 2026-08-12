package io.fleetiq.device.domain.model;

public class DeviceValidationException extends RuntimeException {
    public DeviceValidationException(String message) {
        super(message);
    }
}
