package io.fleetiq.pekko.actor;

import io.fleetiq.pekko.sharding.VehicleSharding;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/** Starts cluster-owned infrastructure. Vehicle entities are created only by sharding. */
public final class FleetGuardian {

    public interface Command {}

    private FleetGuardian() {}

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> {
            VehicleSharding.init(context.getSystem());
            context.getLog().info("Fleet guardian started and vehicle sharding initialized");
            return Behaviors.empty();
        });
    }
}
