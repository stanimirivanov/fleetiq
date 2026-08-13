package io.fleetiq.pekko.api;

import java.time.Instant;
import java.util.concurrent.CompletionStage;

/**
 * Application boundary for per-vehicle state. Callers do not depend on Pekko
 * actor references, sharding, serialization, or ask-pattern details.
 */
public interface VehicleStateService {

    CompletionStage<CommandOutcome> recordTelemetry(TelemetryUpdate update);

    CompletionStage<CommandOutcome> dispatchCommand(VehicleCommand command);

    CompletionStage<VehicleState> getState(String tenantId, String vin);

    record TelemetryUpdate(
        String tenantId,
        String vin,
        Instant observedAt,
        double latitude,
        double longitude,
        double speedKmh
    ) {
        public TelemetryUpdate {
            validateTenant(tenantId);
            VehicleStateValidation.validateVin(vin);
            if (observedAt == null) throw new IllegalArgumentException("observedAt is required");
            if (latitude < -90 || latitude > 90) throw new IllegalArgumentException("Invalid latitude");
            if (longitude < -180 || longitude > 180) throw new IllegalArgumentException("Invalid longitude");
            if (speedKmh < 0) throw new IllegalArgumentException("Speed cannot be negative");
        }
    }

    record VehicleCommand(String tenantId, String vin, String name, String payload) {
        public VehicleCommand {
            validateTenant(tenantId);
            VehicleStateValidation.validateVin(vin);
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Command name is required");
            payload = payload == null ? "" : payload;
        }
    }

    record VehicleState(
        String tenantId,
        String vin,
        Instant lastObservedAt,
        double latitude,
        double longitude,
        double speedKmh,
        long telemetrySequence
    ) {}

    sealed interface CommandOutcome {
        record Accepted(long sequence) implements CommandOutcome {}
        record Rejected(String reason) implements CommandOutcome {}
    }

    private static void validateTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
    }
}
