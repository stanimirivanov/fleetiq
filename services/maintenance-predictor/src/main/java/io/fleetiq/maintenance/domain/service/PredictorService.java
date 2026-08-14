package io.fleetiq.maintenance.domain.service;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.fleetiq.maintenance.domain.port.inbound.PredictMaintenanceUseCase;
import io.fleetiq.maintenance.domain.port.outbound.MaintenanceRepository;
import io.fleetiq.maintenance.domain.port.outbound.PredictionEngine;
import io.fleetiq.maintenance.domain.port.outbound.TelemetryWindowSource;
import io.fleetiq.maintenance.domain.port.outbound.EmbeddingGenerator;
import io.fleetiq.maintenance.domain.port.outbound.EmbeddingStore;
import io.fleetiq.maintenance.domain.model.TelemetryEmbedding;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class PredictorService implements PredictMaintenanceUseCase {

    private final MaintenanceRepository repository;
    private final TelemetryWindowSource telemetryWindowSource;
    private final TelemetryAnomalyDetector anomalyDetector;
    private final PredictionEngine predictionEngine;
    private final EmbeddingGenerator embeddingGenerator;
    private final EmbeddingStore embeddingStore;

    @Override
    public Uni<PredictionResult> predict(String tenantId, String vin, int lookbackDays) {
        validateTenant(tenantId);
        if (vin == null || vin.isBlank()) {
            throw new IllegalArgumentException("VIN is required");
        }
        if (lookbackDays < 1 || lookbackDays > 365) {
            throw new IllegalArgumentException("Lookback days must be between 1 and 365");
        }
        log.info("Predicting maintenance for VIN: {}, lookback: {} days", vin, lookbackDays);
        Instant to = Instant.now();
        Instant from = to.minus(lookbackDays, ChronoUnit.DAYS);

        return telemetryWindowSource.load(tenantId, vin, from, to)
            .flatMap(window -> {
                var assessment = anomalyDetector.assess(window);
                String content = evidenceContent(assessment);
                return embeddingGenerator.generate(content)
                    .map(vector -> new TelemetryEmbedding(
                        UUID.randomUUID(), vin, window.from(), window.to(), vector,
                        assessment.failureProbability(), content, null,
                        Map.of("component", assessment.component(), "severity", assessment.severity().name())))
                    .flatMap(value -> embeddingStore.save(tenantId, value))
                    .flatMap(saved -> embeddingStore.findSimilar(
                        tenantId, vin, saved.embedding(), 5, saved.id()))
                    .flatMap(similar -> predictionEngine.generate(tenantId, vin, assessment, similar));
            })
            .flatMap(prediction -> repository.savePrediction(tenantId, prediction));
    }

    @Override
    public Uni<MaintenanceRecord> recordEvent(String tenantId, MaintenanceRecord record) {
        validateTenant(tenantId);
        log.info("Recording maintenance event for VIN: {}", record.vin());
        return repository.saveEvent(tenantId, record);
    }

    @Override
    public Uni<List<PredictionResult>> getHistory(String tenantId, String vin, int limit) {
        validateTenant(tenantId);
        return repository.findPredictionsByVin(tenantId, vin, limit);
    }

    private static void validateTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
    }

    private static String evidenceContent(io.fleetiq.maintenance.domain.model.AnomalyAssessment assessment) {
        return "component=" + assessment.component()
            + "; severity=" + assessment.severity()
            + "; probability=" + assessment.failureProbability()
            + "; recommendation=" + assessment.recommendation()
            + "; evidence=" + String.join(",", assessment.evidenceIds());
    }
}
