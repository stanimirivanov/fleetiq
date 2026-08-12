package io.fleetiq.telemetry.domain.service;

import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase;
import io.fleetiq.telemetry.domain.port.outbound.TelemetryRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class TelemetryIngestionService implements IngestTelemetryUseCase {

    private final TelemetryRepository telemetryRepository;

    @Override
    public Uni<IngestResult> ingest(TelemetrySample sample) {
        log.debug("Ingesting telemetry for VIN: {}", sample.vin());
        return telemetryRepository.save(sample)
            .map(ignored -> new IngestResult(true, "Telemetry accepted"))
            .onFailure().recoverWithItem(e -> {
                log.error("Failed to ingest telemetry for VIN: {}", sample.vin(), e);
                return new IngestResult(false, "Ingestion failed: " + e.getMessage());
            });
    }

    @Override
    public Uni<List<TelemetrySample>> getTelemetryRange(String vin, Instant from, Instant to) {
        return telemetryRepository.findByVinAndTimeRange(vin, from, to);
    }

    @Override
    public Uni<Double> getAverageSpeed(String vin, Instant from, Instant to) {
        return telemetryRepository.getAverageSpeed(vin, from, to);
    }
}
