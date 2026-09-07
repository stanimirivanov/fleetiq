package io.fleetiq.maintenance.adapter.outbound.ai;

import io.fleetiq.maintenance.domain.model.AnomalyAssessment;
import io.fleetiq.maintenance.domain.model.RecommendationAdvice;
import io.fleetiq.maintenance.domain.model.Severity;
import io.fleetiq.maintenance.domain.model.SimilarIncident;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagPredictionEngineTest {

    private static final String TELEMETRY_EVIDENCE = "telemetry:max-engine-temperature-c=114.00";
    private static final UUID INCIDENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void preservesDeterministicRiskFieldsAndAcceptsAuthorizedCitations() {
        var model = new FakeModel(new RecommendationAdvice(
            "Inspect coolant circulation within seven days", List.of(INCIDENT_ID.toString())));
        var engine = new RagPredictionEngine(model);

        var result = engine.generate("tenant-a", "VIN-1", assessment(), List.of(incident()))
            .await().indefinitely();

        assertEquals(0.8, result.failureProbability());
        assertEquals(Severity.HIGH, result.severity());
        assertEquals("engine-cooling", result.predictedComponent());
        assertEquals(List.of(TELEMETRY_EVIDENCE, INCIDENT_ID.toString()), result.evidenceIds());
    }

    @Test
    void rejectsCitationsThatWereNotInTheAuthorizedPrompt() {
        var model = new FakeModel(new RecommendationAdvice("Unsafe advice", List.of("unknown")));
        var engine = new RagPredictionEngine(model);

        assertThrows(IllegalArgumentException.class, () ->
            engine.generate("tenant-a", "VIN-1", assessment(), List.of(incident()))
                .await().indefinitely());
    }

    private static AnomalyAssessment assessment() {
        return new AnomalyAssessment(0.8, "engine-cooling", Severity.HIGH, 7,
            "Inspect cooling system", List.of(TELEMETRY_EVIDENCE));
    }

    private static SimilarIncident incident() {
        return new SimilarIncident(UUID.randomUUID(), INCIDENT_ID, 0.1,
            "Previous coolant pump failure", Map.of());
    }

    private record FakeModel(RecommendationAdvice advice)
        implements io.fleetiq.maintenance.domain.port.outbound.RecommendationModel {
        @Override
        public Uni<RecommendationAdvice> recommend(String vin, AnomalyAssessment assessment,
                                                    List<SimilarIncident> similarIncidents) {
            return Uni.createFrom().item(advice);
        }
    }
}
