package io.fleetiq.pekko.actor;

import io.fleetiq.pekko.api.VehicleStateService.TelemetryUpdate;
import io.fleetiq.pekko.api.VehicleStateService.VehicleCommand;
import io.fleetiq.pekko.serialization.CborSerializable;
import io.fleetiq.pekko.sharding.VehicleSharding;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.persistence.typed.PersistenceId;
import org.apache.pekko.persistence.typed.javadsl.CommandHandler;
import org.apache.pekko.persistence.typed.javadsl.EventHandler;
import org.apache.pekko.persistence.typed.javadsl.EventSourcedBehavior;
import org.apache.pekko.persistence.typed.javadsl.RetentionCriteria;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Durable owner of the current state for one tenant-scoped vehicle.
 *
 * <p>The actor accepts ordered telemetry and idempotent vehicle commands. It stores facts as
 * events, so its state can be rebuilt after an actor or node restart. Callers should use
 * {@code VehicleStateService}; the nested protocol is the internal Pekko boundary.</p>
 */
public final class VehicleActor extends EventSourcedBehavior<VehicleActor.Command, VehicleActor.Event, VehicleActor.State> {

    private static final int COMMAND_ID_HISTORY_LIMIT = 1_000;
    private static final int SNAPSHOT_EVERY_EVENTS = 100;
    private static final int SNAPSHOTS_TO_KEEP = 2;

    private final String tenantId;
    private final String vin;

    public sealed interface Command extends CborSerializable permits RecordTelemetry, DispatchCommand, GetState {}

    public record RecordTelemetry(TelemetryUpdate update, ActorRef<OutcomeReply> replyTo) implements Command {}

    public record DispatchCommand(VehicleCommand command, ActorRef<OutcomeReply> replyTo) implements Command {}

    public record GetState(ActorRef<StateReply> replyTo) implements Command {}

    public sealed interface Event extends CborSerializable permits TelemetryRecorded, VehicleCommandAccepted {}

    public record TelemetryRecorded(
        Instant observedAt, double latitude, double longitude, double speedKmh
    ) implements Event {}

    public record VehicleCommandAccepted(String commandId, String name, String payload) implements Event {}

    /** Serializable state reconstructed by replaying persisted events. */
    public record State(
        String tenantId,
        String vin,
        Instant lastObservedAt,
        double latitude,
        double longitude,
        double speedKmh,
        long telemetrySequence,
        long eventSequence,
        List<String> recentCommandIds
    ) implements CborSerializable {
        public State {
            recentCommandIds = List.copyOf(recentCommandIds);
        }

        private State record(TelemetryRecorded event) {
            return new State(tenantId, vin, event.observedAt(), event.latitude(), event.longitude(),
                event.speedKmh(), telemetrySequence + 1, eventSequence + 1, recentCommandIds);
        }

        private State accept(VehicleCommandAccepted event) {
            var commandIds = new ArrayList<>(recentCommandIds);
            commandIds.add(event.commandId());
            if (commandIds.size() > COMMAND_ID_HISTORY_LIMIT) {
                commandIds.removeFirst();
            }
            return new State(tenantId, vin, lastObservedAt, latitude, longitude, speedKmh,
                telemetrySequence, eventSequence + 1, commandIds);
        }
    }

    public record OutcomeReply(boolean accepted, long sequence, String reason) implements CborSerializable {
        static OutcomeReply accepted(long sequence) {
            return new OutcomeReply(true, sequence, "");
        }

        static OutcomeReply rejected(long sequence, String reason) {
            return new OutcomeReply(false, sequence, reason);
        }
    }

    public record StateReply(
        String tenantId,
        String vin,
        Instant lastObservedAt,
        double latitude,
        double longitude,
        double speedKmh,
        long telemetrySequence
    ) implements CborSerializable {}

    public static Behavior<Command> create(String tenantId, String vin) {
        return Behaviors.setup(ignored -> new VehicleActor(tenantId, vin));
    }

    private VehicleActor(String tenantId, String vin) {
        super(PersistenceId.of("Vehicle", VehicleSharding.encode(tenantId, vin)));
        this.tenantId = tenantId;
        this.vin = vin;
    }

    @Override
    public State emptyState() {
        return new State(tenantId, vin, null, 0, 0, 0, 0, 0, List.of());
    }

    @Override
    public CommandHandler<Command, Event, State> commandHandler() {
        return newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(RecordTelemetry.class, this::onRecordTelemetry)
            .onCommand(DispatchCommand.class, this::onDispatchCommand)
            .onCommand(GetState.class, this::onGetState)
            .build();
    }

    private org.apache.pekko.persistence.typed.javadsl.Effect<Event, State> onRecordTelemetry(
        State state, RecordTelemetry message
    ) {
        TelemetryUpdate update = message.update();
        if (!matchesIdentity(update.tenantId(), update.vin())) {
            message.replyTo().tell(OutcomeReply.rejected(
                state.telemetrySequence(), "Tenant or VIN does not match entity identity"));
            return Effect().none();
        }
        if (state.lastObservedAt() != null) {
            int order = update.observedAt().compareTo(state.lastObservedAt());
            if (order < 0) {
                message.replyTo().tell(OutcomeReply.rejected(
                    state.telemetrySequence(), "Telemetry is older than current state"));
                return Effect().none();
            }
            if (order == 0) {
                boolean duplicate = Double.compare(update.latitude(), state.latitude()) == 0
                    && Double.compare(update.longitude(), state.longitude()) == 0
                    && Double.compare(update.speedKmh(), state.speedKmh()) == 0;
                message.replyTo().tell(duplicate
                    ? OutcomeReply.accepted(state.telemetrySequence())
                    : OutcomeReply.rejected(state.telemetrySequence(),
                        "Telemetry timestamp conflicts with current state"));
                return Effect().none();
            }
        }

        var event = new TelemetryRecorded(
            update.observedAt(), update.latitude(), update.longitude(), update.speedKmh());
        return Effect().persist(event)
            .thenRun(updated -> message.replyTo().tell(OutcomeReply.accepted(updated.telemetrySequence())));
    }

    private org.apache.pekko.persistence.typed.javadsl.Effect<Event, State> onDispatchCommand(
        State state, DispatchCommand message
    ) {
        VehicleCommand command = message.command();
        if (!matchesIdentity(command.tenantId(), command.vin())) {
            message.replyTo().tell(OutcomeReply.rejected(
                state.eventSequence(), "Tenant or VIN does not match entity identity"));
            return Effect().none();
        }
        if (state.recentCommandIds().contains(command.commandId())) {
            message.replyTo().tell(OutcomeReply.accepted(state.eventSequence()));
            return Effect().none();
        }

        var event = new VehicleCommandAccepted(command.commandId(), command.name(), command.payload());
        return Effect().persist(event)
            .thenRun(updated -> message.replyTo().tell(OutcomeReply.accepted(updated.eventSequence())));
    }

    private org.apache.pekko.persistence.typed.javadsl.Effect<Event, State> onGetState(
        State state, GetState message
    ) {
        message.replyTo().tell(new StateReply(state.tenantId(), state.vin(), state.lastObservedAt(),
            state.latitude(), state.longitude(), state.speedKmh(), state.telemetrySequence()));
        return Effect().none();
    }

    @Override
    public EventHandler<State, Event> eventHandler() {
        return newEventHandlerBuilder()
            .forAnyState()
            .onEvent(TelemetryRecorded.class, State::record)
            .onEvent(VehicleCommandAccepted.class, State::accept)
            .build();
    }

    @Override
    public RetentionCriteria retentionCriteria() {
        return RetentionCriteria.snapshotEvery(SNAPSHOT_EVERY_EVENTS, SNAPSHOTS_TO_KEEP);
    }

    private boolean matchesIdentity(String candidateTenantId, String candidateVin) {
        return tenantId.equals(candidateTenantId) && vin.equals(candidateVin);
    }
}
