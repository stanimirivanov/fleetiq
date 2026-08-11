package io.fleetiq.device.adapter.outbound.persistence;

import io.fleetiq.device.domain.model.Device;
import io.fleetiq.device.domain.port.outbound.DeviceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Optional;

@ApplicationScoped
public class PostgresDeviceRepository implements DeviceRepository {

    private static final Logger log = LoggerFactory.getLogger(PostgresDeviceRepository.class);
    private final DataSource dataSource;

    public PostgresDeviceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Device device) {
        log.debug("Saving device: {}", device.vin());
    }

    @Override
    public Optional<Device> findByVin(String vin) {
        log.debug("Finding device by VIN: {}", vin);
        return Optional.empty();
    }

    @Override
    public void updateStatus(String vin, String status) {
        log.debug("Updating device status: {} -> {}", vin, status);
    }
}
