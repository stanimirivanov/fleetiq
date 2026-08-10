package io.fleetiq.telemetry.adapter.outbound.persistence;

import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.fleetiq.telemetry.domain.port.outbound.TelemetryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

@ApplicationScoped
public class TimescaleTelemetryRepository implements TelemetryRepository {

    private static final Logger log = LoggerFactory.getLogger(TimescaleTelemetryRepository.class);

    private final DataSource dataSource;

    public TimescaleTelemetryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(TelemetrySample sample) {
        // Placeholder: will use proper JDBC/Hibernate in implementation
        log.debug("Persisting telemetry for VIN: {}", sample.vin());
    }
}
