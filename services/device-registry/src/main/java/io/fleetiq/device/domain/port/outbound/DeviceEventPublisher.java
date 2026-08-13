package io.fleetiq.device.domain.port.outbound;

import io.fleetiq.device.domain.model.Device;
import io.smallrye.mutiny.Uni;

public interface DeviceEventPublisher {
    Uni<Void> publish(Device device);
}
