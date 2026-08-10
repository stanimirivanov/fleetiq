package io.fleetiq.telemetry.domain.service;

import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase;
import io.fleetiq.telemetry.domain.port.outbound.TelemetryRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TelemetryIngestionService implements IngestTelemetryUseCase {

    private final TelemetryRepository telemetryRepository;

    public TelemetryIngestionService(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    @Override
    public IngestResult ingest(TelemetrySample sample) {
        try {
            telemetryRepository.save(sample);
            return new IngestResult(true, "Telemetry accepted");
        } catch (Exception e) {
            return new IngestResult(false, "Ingestion failed: " + e.getMessage());
        }
    }
}
