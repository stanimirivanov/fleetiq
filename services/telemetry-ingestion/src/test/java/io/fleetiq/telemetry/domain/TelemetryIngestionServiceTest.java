package io.fleetiq.telemetry.domain;

import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase;
import io.fleetiq.telemetry.domain.port.outbound.TelemetryRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for TelemetryIngestionService.
 * Uses Mockito to mock the outbound repository port.
 */
@QuarkusTest
@DisplayName("Telemetry Ingestion Service Unit Tests")
class TelemetryIngestionServiceTest {

    @Inject
    IngestTelemetryUseCase service;

    @InjectMock
    TelemetryRepository repository;

    @BeforeEach
    void setUp() {
        reset(repository);
    }

    @Test
    @DisplayName("Should successfully ingest and persist telemetry sample")
    void shouldPersistValidSample() {
        // Given
        TelemetrySample sample = new TelemetrySample(
            "VIN-UNIT-001",
            Instant.now(),
            51.5074,    // London
            -0.1278,
            20.0,
            65.0,
            50.0,
            95.0,
            12.8,
            Map.of()
        );

        doNothing().when(repository).save(any());

        // When
        IngestTelemetryUseCase.IngestResult result = service.ingest(sample);

        // Then
        assertTrue(result.accepted());
        verify(repository, times(1)).save(sample);
    }

    @Test
    @DisplayName("Should return failure when repository throws exception")
    void shouldReturnFailureOnRepositoryError() {
        // Given
        TelemetrySample sample = new TelemetrySample(
            "VIN-UNIT-002",
            Instant.now(),
            40.7128,    // New York
            -74.0060,
            15.0,
            45.0,
            60.0,
            90.0,
            13.0,
            Map.of()
        );

        doThrow(new RuntimeException("Database connection lost"))
            .when(repository).save(any());

        // When
        IngestTelemetryUseCase.IngestResult result = service.ingest(sample);

        // Then
        assertFalse(result.accepted());
        assertTrue(result.message().contains("Ingestion failed"));
        verify(repository, times(1)).save(sample);
    }

    @Test
    @DisplayName("Should handle telemetry with large custom metrics map")
    void shouldHandleLargeCustomMetrics() {
        // Given
        Map<String, Double> largeMetrics = new java.util.HashMap<>();
        for (int i = 0; i < 100; i++) {
            largeMetrics.put("metric_" + i, Math.random() * 100);
        }

        TelemetrySample sample = new TelemetrySample(
            "VIN-UNIT-003",
            Instant.now(),
            35.6762,    // Tokyo
            139.6503,
            10.0,
            30.0,
            40.0,
            85.0,
            11.5,
            largeMetrics
        );

        doNothing().when(repository).save(any());

        // When
        IngestTelemetryUseCase.IngestResult result = service.ingest(sample);

        // Then
        assertTrue(result.accepted());
        verify(repository).save(sample);
    }
}
