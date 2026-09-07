package io.fleetiq.maintenance.domain.service;

import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.fleetiq.maintenance.domain.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationPublicationPolicyTest {

    @Test
    void publishesOnlyAtOrAboveConfiguredThreshold() {
        var policy = new RecommendationPublicationPolicy(0.8);

        assertFalse(policy.shouldPublish(prediction(0.799)));
        assertTrue(policy.shouldPublish(prediction(0.8)));
    }

    @Test
    void rejectsThresholdOutsideProbabilityRange() {
        assertThrows(IllegalArgumentException.class,
            () -> new RecommendationPublicationPolicy(1.01));
    }

    private static PredictionResult prediction(double probability) {
        return new PredictionResult(UUID.randomUUID(), "WVWZZZ1JZXW000001", probability,
            "engine-cooling", Severity.HIGH, 7, "Inspect cooling system", List.of());
    }
}
