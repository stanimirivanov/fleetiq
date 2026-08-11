package io.fleetiq.pekko.sharding;

import io.fleetiq.pekko.actor.VehicleActor;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.Entity;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityRef;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityTypeKey;

public class VehicleSharding {

    public static final EntityTypeKey<VehicleActor.Command> VEHICLE_ENTITY_KEY =
        EntityTypeKey.create(VehicleActor.Command.class, "Vehicle");

    public static void init(ActorSystem<?> system) {
        ClusterSharding.get(system).init(
            Entity.of(VEHICLE_ENTITY_KEY, ctx -> VehicleActor.create(ctx.getEntityId()))
        );
    }

    public static EntityRef<VehicleActor.Command> getVehicleRef(
        ActorSystem<?> system, String vin) {
        return ClusterSharding.get(system).entityRefFor(VEHICLE_ENTITY_KEY, vin);
    }
}
