package io.fleetiq.maintenance.domain.service;

import io.fleetiq.maintenance.domain.model.PredictionResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Decides which advisory predictions are important enough to notify downstream consumers. */
@ApplicationScoped
public class RecommendationPublicationPolicy {

    private final double threshold;

    @Inject
    public RecommendationPublicationPolicy(
        @ConfigProperty(name = "fleetiq.maintenance.publication-threshold", defaultValue = "0.8")
        double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Recommendation publication threshold must be between 0 and 1");
        }
        this.threshold = threshold;
    }

    public boolean shouldPublish(PredictionResult prediction) {
        return prediction.failureProbability() >= threshold;
    }
}
