package io.fleetiq.pekko.actor;

import io.fleetiq.pekko.api.VehicleStateService.TelemetryUpdate;
import io.fleetiq.pekko.api.VehicleStateService.VehicleCommand;
import io.fleetiq.pekko.serialization.CborSerializable;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

import java.time.Instant;

public class VehicleActor extends AbstractBehavior<VehicleActor.Command> {

    public sealed interface Command extends CborSerializable {}

    public record RecordTelemetry(TelemetryUpdate update, ActorRef<OutcomeReply> replyTo)
        implements Command {}

    public record DispatchCommand(VehicleCommand command, ActorRef<OutcomeReply> replyTo)
        implements Command {}

    public record GetState(ActorRef<StateReply> replyTo) implements Command {}

    public record OutcomeReply(boolean accepted, long sequence, String reason) implements CborSerializable {
        static OutcomeReply accepted(long sequence) {
            return new OutcomeReply(true, sequence, "");
        }

        static OutcomeReply rejected(String reason) {
            return new OutcomeReply(false, 0, reason);
        }
    }

    public record StateReply(
        String vin,
        Instant lastObservedAt,
        double latitude,
        double longitude,
        double speedKmh,
        long telemetrySequence
    ) implements CborSerializable {}

    private final String vin;
    private Instant lastObservedAt;
    private double lastLatitude;
    private double lastLongitude;
    private double lastSpeed;
    private long telemetrySequence;

    public static Behavior<Command> create(String vin) {
        return Behaviors.setup(ctx -> new VehicleActor(ctx, vin));
    }

    private VehicleActor(ActorContext<Command> context, String vin) {
        super(context);
        this.vin = vin;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(RecordTelemetry.class, this::onTelemetry)
            .onMessage(DispatchCommand.class, this::onCommand)
            .onMessage(GetState.class, this::onGetState)
            .build();
    }

    private Behavior<Command> onTelemetry(RecordTelemetry message) {
        TelemetryUpdate update = message.update();
        if (!vin.equals(update.vin())) {
            message.replyTo().tell(OutcomeReply.rejected("VIN does not match entity identity"));
            return this;
        }
        if (lastObservedAt != null && update.observedAt().isBefore(lastObservedAt)) {
            message.replyTo().tell(OutcomeReply.rejected("Telemetry is older than current state"));
            return this;
        }
        lastObservedAt = update.observedAt();
        lastLatitude = update.latitude();
        lastLongitude = update.longitude();
        lastSpeed = update.speedKmh();
        telemetrySequence++;
        message.replyTo().tell(OutcomeReply.accepted(telemetrySequence));
        return this;
    }

    private Behavior<Command> onCommand(DispatchCommand message) {
        if (!vin.equals(message.command().vin())) {
            message.replyTo().tell(OutcomeReply.rejected("VIN does not match entity identity"));
        } else {
            message.replyTo().tell(OutcomeReply.accepted(telemetrySequence));
        }
        return this;
    }

    private Behavior<Command> onGetState(GetState message) {
        message.replyTo().tell(new StateReply(
            vin, lastObservedAt, lastLatitude, lastLongitude, lastSpeed, telemetrySequence));
        return this;
    }
}
