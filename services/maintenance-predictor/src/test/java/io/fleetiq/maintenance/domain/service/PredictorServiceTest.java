package io.fleetiq.maintenance.domain.service;

import io.fleetiq.maintenance.domain.model.AnomalyAssessment;
import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.fleetiq.maintenance.domain.model.TelemetryReading;
import io.fleetiq.maintenance.domain.model.TelemetryWindow;
import io.fleetiq.maintenance.domain.port.outbound.MaintenanceRepository;
import io.fleetiq.maintenance.domain.port.outbound.PredictionEngine;
import io.fleetiq.maintenance.domain.port.outbound.TelemetryWindowSource;
import io.fleetiq.maintenance.domain.port.outbound.EmbeddingStore;
import io.fleetiq.maintenance.domain.model.GeneratedEmbedding;
import io.fleetiq.maintenance.domain.model.SimilarIncident;
import io.fleetiq.maintenance.domain.model.TelemetryEmbedding;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PredictorServiceTest {

    @Test
    void loadsAuthorizedTelemetryScoresItAndPersistsPrediction() {
        AtomicReference<PredictionResult> saved = new AtomicReference<>();
        MaintenanceRepository repository = repository(saved);
        TelemetryWindowSource source = (tenant, vin, from, to) -> Uni.createFrom().item(
            new TelemetryWindow(vin, from, to, List.of(
                new TelemetryReading(to.minusSeconds(30), 114.0, 12.5, 72.0))));
        PredictionEngine engine = (tenant, vin, assessment, similar) ->
            Uni.createFrom().item(toPrediction(vin, assessment));
        var service = new PredictorService(repository, source, new TelemetryAnomalyDetector(), engine,
            content -> Uni.createFrom().item(new GeneratedEmbedding(
                "test-model", "1", 3, List.of(1.0f, 0.0f, 0.0f))),
            embeddingStore());

        PredictionResult result = service.predict("tenant-a", "WVWZZZ1JZXW000001", 7)
            .await().indefinitely();

        assertSame(result, saved.get());
        assertEquals("engine-cooling", result.predictedComponent());
        assertEquals(0.8, result.failureProbability());
        assertEquals(List.of("telemetry:max-engine-temperature-c=114.00"), result.evidenceIds());
    }

    private static EmbeddingStore embeddingStore() {
        return new EmbeddingStore() {
            @Override
            public Uni<TelemetryEmbedding> save(String tenantId, TelemetryEmbedding embedding) {
                return Uni.createFrom().item(embedding);
            }

            @Override
            public Uni<List<SimilarIncident>> findSimilar(String tenantId, String vin,
                    GeneratedEmbedding query, int limit, UUID excludingId) {
                return Uni.createFrom().item(List.of());
            }
        };
    }

    private static PredictionResult toPrediction(String vin, AnomalyAssessment assessment) {
        return new PredictionResult(
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
            vin,
            assessment.failureProbability(),
            assessment.component(),
            assessment.severity(),
            assessment.estimatedDaysUntilFailure(),
            assessment.recommendation(),
            assessment.evidenceIds());
    }

    private static MaintenanceRepository repository(AtomicReference<PredictionResult> saved) {
        return new MaintenanceRepository() {
            @Override
            public Uni<MaintenanceRecord> saveEvent(String tenantId, MaintenanceRecord record) {
                return Uni.createFrom().item(record);
            }

            @Override
            public Uni<PredictionResult> savePrediction(String tenantId, PredictionResult prediction) {
                saved.set(prediction);
                return Uni.createFrom().item(prediction);
            }

            @Override
            public Uni<List<MaintenanceRecord>> findEventsByVin(String tenantId, String vin) {
                return Uni.createFrom().item(List.of());
            }

            @Override
            public Uni<List<PredictionResult>> findPredictionsByVin(String tenantId, String vin, int limit) {
                return Uni.createFrom().item(List.of());
            }
        };
    }
}
