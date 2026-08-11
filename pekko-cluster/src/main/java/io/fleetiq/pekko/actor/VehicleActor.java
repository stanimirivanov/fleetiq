package io.fleetiq.pekko.actor;

import io.fleetiq.pekko.serialization.CborSerializable;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class VehicleActor extends AbstractBehavior<VehicleActor.Command> {

    public interface Command extends CborSerializable {}

    public record TelemetryReceived(
        String vin, double latitude, double longitude, double speedKmh
    ) implements Command {}

    public record SendCommand(String vin, String command, String payload) implements Command {}

    public record GetState(String replyTo) implements Command {}

    private final String vin;
    private double lastLatitude;
    private double lastLongitude;
    private double lastSpeed;

    public static Behavior<Command> create(String vin) {
        return Behaviors.setup(ctx -> new VehicleActor(ctx, vin));
    }

    public VehicleActor(ActorContext<Command> context, String vin) {
        super(context);
        this.vin = vin;
        context.getLog().info("Vehicle actor created: {}", vin);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(TelemetryReceived.class, this::onTelemetryReceived)
            .onMessage(SendCommand.class, this::onSendCommand)
            .onMessage(GetState.class, this::onGetState)
            .build();
    }

    private Behavior<Command> onTelemetryReceived(TelemetryReceived msg) {
        this.lastLatitude = msg.latitude();
        this.lastLongitude = msg.longitude();
        this.lastSpeed = msg.speedKmh();
        getContext().getLog().debug("Telemetry updated for {}: ({}, {}) {} km/h",
            vin, lastLatitude, lastLongitude, lastSpeed);
        return this;
    }

    private Behavior<Command> onSendCommand(SendCommand msg) {
        getContext().getLog().info("Command sent to {}: {}", vin, msg.command());
        return this;
    }

    private Behavior<Command> onGetState(GetState msg) {
        getContext().getLog().debug("State requested for {}", vin);
        return this;
    }
}
