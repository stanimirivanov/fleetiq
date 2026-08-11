package io.fleetiq.telemetry.domain;

import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase;
import io.fleetiq.telemetry.domain.port.outbound.TelemetryRepository;
import io.fleetiq.telemetry.domain.service.TelemetryIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Telemetry Ingestion Service Unit Tests")
class TelemetryIngestionServiceTest {

    private IngestTelemetryUseCase service;

    @BeforeEach
    void setUp() {
        // Stub repository that does nothing
        TelemetryRepository repository = sample -> {};
        service = new TelemetryIngestionService(repository);
    }

    @Test
    @DisplayName("Should accept valid telemetry sample")
    void shouldAcceptValidSample() {
        TelemetrySample sample = new TelemetrySample(
            "VIN-UNIT-001", Instant.now(),
            51.5074, -0.1278, 20.0,
            65.0, 50.0, 95.0, 12.8, Map.of()
        );

        IngestTelemetryUseCase.IngestResult result = service.ingest(sample);

        assertTrue(result.accepted());
        assertEquals("Telemetry accepted", result.message());
    }

    @Test
    @DisplayName("Should handle telemetry with custom metrics")
    void shouldHandleCustomMetrics() {
        Map<String, Double> metrics = new java.util.HashMap<>();
        for (int i = 0; i < 100; i++) {
            metrics.put("metric_" + i, Math.random() * 100);
        }

        TelemetrySample sample = new TelemetrySample(
            "VIN-UNIT-002", Instant.now(),
            35.6762, 139.6503, 10.0,
            30.0, 40.0, 85.0, 11.5, metrics
        );

        IngestTelemetryUseCase.IngestResult result = service.ingest(sample);

        assertTrue(result.accepted());
    }
}
