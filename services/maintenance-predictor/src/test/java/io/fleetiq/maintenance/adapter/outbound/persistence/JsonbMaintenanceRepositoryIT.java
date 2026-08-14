package io.fleetiq.maintenance.adapter.outbound.persistence;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.model.Severity;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.fleetiq.maintenance.domain.model.GeneratedEmbedding;
import io.fleetiq.maintenance.domain.model.TelemetryEmbedding;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class JsonbMaintenanceRepositoryIT {
    private static final String VIN = "1HGCM82633A004352";

    @Inject JsonbMaintenanceRepository repository;
    @Inject PgVectorEmbeddingStore embeddingStore;
    @Inject PgPool pgPool;

    @Test
    @RunOnVertxContext
    void isolatesIdenticalVehicleHistoryAcrossTenants(UniAsserter asserter) {
        asserter.execute(() -> pgPool.query(
            "TRUNCATE TABLE maintenance_events, maintenance_predictions, telemetry_embeddings").execute());
        asserter.execute(() -> repository.saveEvent("tenant-a", event("brakes")));
        asserter.execute(() -> repository.saveEvent("tenant-b", event("battery")));
        asserter.assertThat(() -> repository.findEventsByVin("tenant-a", VIN), events -> {
            assertEquals(1, events.size());
            assertEquals("brakes", events.getFirst().component());
        });
        asserter.assertThat(() -> repository.findEventsByVin("tenant-b", VIN), events -> {
            assertEquals(1, events.size());
            assertEquals("battery", events.getFirst().component());
        });
    }

    @Test
    @RunOnVertxContext
    void persistsStructuredPredictionSeverityAndEvidence(UniAsserter asserter) {
        var prediction = new PredictionResult(UUID.randomUUID(), VIN, 0.8,
            "engine-cooling", Severity.HIGH, 7, "Inspect cooling system",
            java.util.List.of("telemetry:max-engine-temperature-c=114.00"));

        asserter.execute(() -> pgPool.query(
            "TRUNCATE TABLE maintenance_events, maintenance_predictions, telemetry_embeddings").execute());
        asserter.execute(() -> repository.savePrediction("tenant-a", prediction));
        asserter.assertThat(() -> repository.findPredictionsByVin("tenant-a", VIN, 10), predictions -> {
            assertEquals(1, predictions.size());
            assertEquals(Severity.HIGH, predictions.getFirst().severity());
            assertEquals(prediction.evidenceIds(), predictions.getFirst().evidenceIds());
        });
    }

    @Test
    @RunOnVertxContext
    void retrievesOnlyTenantAndVinScopedEmbeddingsByCosineDistance(UniAsserter asserter) {
        var query = embedding(1.0f, 0.0f);
        var near = telemetryEmbedding(VIN, embedding(0.9f, 0.1f), "near");
        var far = telemetryEmbedding(VIN, embedding(0.0f, 1.0f), "far");
        var otherTenant = telemetryEmbedding(VIN, embedding(1.0f, 0.0f), "forbidden");
        var otherVin = telemetryEmbedding("JH4KA9650MC000001", embedding(1.0f, 0.0f), "other-vin");

        asserter.execute(() -> pgPool.query(
            "TRUNCATE TABLE maintenance_events, maintenance_predictions, telemetry_embeddings").execute());
        asserter.execute(() -> embeddingStore.save("tenant-a", far));
        asserter.execute(() -> embeddingStore.save("tenant-a", near));
        asserter.execute(() -> embeddingStore.save("tenant-b", otherTenant));
        asserter.execute(() -> embeddingStore.save("tenant-a", otherVin));
        asserter.assertThat(() -> embeddingStore.findSimilar(
            "tenant-a", VIN, query, 10, UUID.randomUUID()), incidents -> {
            assertEquals(2, incidents.size());
            assertEquals("near", incidents.get(0).content());
            assertEquals("far", incidents.get(1).content());
        });
    }

    private static MaintenanceRecord event(String component) {
        return new MaintenanceRecord(UUID.randomUUID(), VIN, component, "inspection",
            Severity.LOW, Instant.parse("2026-08-13T10:00:00Z"), null, Map.of(), Map.of());
    }

    private static GeneratedEmbedding embedding(float first, float second) {
        var values = new java.util.ArrayList<Float>(java.util.Collections.nCopies(384, 0.0f));
        values.set(0, first);
        values.set(1, second);
        return new GeneratedEmbedding("sentence-transformers/all-MiniLM-L6-v2",
            "langchain4j-1.18.1-beta28", 384, values);
    }

    private static TelemetryEmbedding telemetryEmbedding(String vin, GeneratedEmbedding embedding, String content) {
        return new TelemetryEmbedding(UUID.randomUUID(), vin,
            Instant.parse("2026-08-14T10:00:00Z"), Instant.parse("2026-08-14T11:00:00Z"),
            embedding, 0.7, content, null, Map.of("source", "test"));
    }
}
