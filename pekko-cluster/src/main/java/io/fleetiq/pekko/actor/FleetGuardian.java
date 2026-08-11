package io.fleetiq.pekko.actor;

import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.japi.function.Function;

public class FleetGuardian extends AbstractBehavior<FleetGuardian.Command> {

    public interface Command {}

    public record CreateVehicle(String vin) implements Command {}

    public static Behavior<Command> create() {
        return Behaviors.setup(FleetGuardian::new);
    }

    public FleetGuardian(ActorContext<Command> context) {
        super(context);
        context.getLog().info("Fleet Guardian started");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(CreateVehicle.class, this::onCreateVehicle)
            .build();
    }

    private Behavior<Command> onCreateVehicle(CreateVehicle msg) {
        getContext().getLog().info("Spawning vehicle actor: {}", msg.vin());
        getContext().spawn(VehicleActor.create(msg.vin()), "vehicle-" + msg.vin());
        return this;
    }
}
