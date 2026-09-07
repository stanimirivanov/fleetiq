package io.fleetiq.pekko.api;

import java.time.Instant;
import java.util.concurrent.CompletionStage;

/**
 * Application boundary for per-vehicle state. Callers do not depend on Pekko
 * actor references, sharding, serialization, or ask-pattern details.
 */
public interface VehicleStateService {

    /**
     * Applies telemetry in timestamp order. Replaying an identical observation is idempotent;
     * older or conflicting observations are returned as rejected outcomes.
     */
    CompletionStage<CommandOutcome> recordTelemetry(TelemetryUpdate update);

    /**
     * Dispatches a vehicle command. A recently seen command ID is accepted without applying the
     * command twice, which lets callers retry after an uncertain response.
     */
    CompletionStage<CommandOutcome> dispatchCommand(VehicleCommand command);

    /**
     * Reads the current state for a tenant-scoped vehicle. A newly activated vehicle has no last
     * observation and reports a telemetry sequence of zero.
     */
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

    record VehicleCommand(String tenantId, String vin, String name, String payload, String commandId) {
        public VehicleCommand {
            validateTenant(tenantId);
            VehicleStateValidation.validateVin(vin);
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Command name is required");
            payload = payload == null ? "" : payload;
            if (commandId == null || commandId.isBlank()) throw new IllegalArgumentException("commandId is required");
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
