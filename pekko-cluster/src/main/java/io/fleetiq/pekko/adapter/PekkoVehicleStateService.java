package io.fleetiq.pekko.adapter;

import io.fleetiq.pekko.actor.VehicleActor;
import io.fleetiq.pekko.api.VehicleStateService;
import io.fleetiq.pekko.api.VehicleStateValidation;
import io.fleetiq.pekko.sharding.VehicleSharding;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.AskPattern;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

public final class PekkoVehicleStateService implements VehicleStateService {

    private final ActorSystem<?> actorSystem;
    private final Duration askTimeout;

    public PekkoVehicleStateService(ActorSystem<?> actorSystem, Duration askTimeout) {
        this.actorSystem = Objects.requireNonNull(actorSystem);
        this.askTimeout = Objects.requireNonNull(askTimeout);
    }

    @Override
    public CompletionStage<CommandOutcome> recordTelemetry(TelemetryUpdate update) {
        var entity = VehicleSharding.getVehicleRef(actorSystem, update.tenantId(), update.vin());
        return AskPattern.<VehicleActor.Command, VehicleActor.OutcomeReply>ask(
                entity, replyTo -> new VehicleActor.RecordTelemetry(update, replyTo),
                askTimeout, actorSystem.scheduler())
            .thenApply(this::toOutcome);
    }

    @Override
    public CompletionStage<CommandOutcome> dispatchCommand(VehicleCommand command) {
        var entity = VehicleSharding.getVehicleRef(actorSystem, command.tenantId(), command.vin());
        return AskPattern.<VehicleActor.Command, VehicleActor.OutcomeReply>ask(
                entity, replyTo -> new VehicleActor.DispatchCommand(command, replyTo),
                askTimeout, actorSystem.scheduler())
            .thenApply(this::toOutcome);
    }

    @Override
    public CompletionStage<VehicleState> getState(String tenantId, String vin) {
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId is required");
        VehicleStateValidation.validateVin(vin);
        var entity = VehicleSharding.getVehicleRef(actorSystem, tenantId, vin);
        return AskPattern.<VehicleActor.Command, VehicleActor.StateReply>ask(
                entity, VehicleActor.GetState::new, askTimeout, actorSystem.scheduler())
            .thenApply(state -> new VehicleState(
                state.tenantId(), state.vin(), state.lastObservedAt(), state.latitude(), state.longitude(),
                state.speedKmh(), state.telemetrySequence()));
    }

    private CommandOutcome toOutcome(VehicleActor.OutcomeReply reply) {
        return reply.accepted()
            ? new CommandOutcome.Accepted(reply.sequence())
            : new CommandOutcome.Rejected(reply.reason());
    }
}
