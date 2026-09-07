package io.fleetiq.maintenance.adapter.inbound.scheduler;

import io.fleetiq.maintenance.domain.port.inbound.PredictMaintenanceUseCase;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder scheduling boundary for future fleet-wide prediction orchestration.
 * The baseline intentionally logs the trigger only: it has no safe tenant/device enumeration
 * source yet, so invoking predictions here would require inventing identity or ownership data.
 */
@ApplicationScoped
public class PredictionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PredictionScheduler.class);

    @Inject
    PredictMaintenanceUseCase predictor;

    @Scheduled(every = "6h")
    void runPrediction() {
        log.info("Scheduled maintenance prediction triggered");
    }
}
