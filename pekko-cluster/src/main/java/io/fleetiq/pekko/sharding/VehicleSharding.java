package io.fleetiq.pekko.sharding;

import io.fleetiq.pekko.actor.VehicleActor;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.Entity;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityRef;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityTypeKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class VehicleSharding {

    public static final EntityTypeKey<VehicleActor.Command> VEHICLE_ENTITY_KEY =
        EntityTypeKey.create(VehicleActor.Command.class, "Vehicle");

    public static void init(ActorSystem<?> system) {
        ClusterSharding.get(system).init(
            Entity.of(VEHICLE_ENTITY_KEY, ctx -> {
                VehicleIdentity identity = decode(ctx.getEntityId());
                return VehicleActor.create(identity.tenantId(), identity.vin());
            })
        );
    }

    public static EntityRef<VehicleActor.Command> getVehicleRef(
        ActorSystem<?> system, String tenantId, String vin) {
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId is required");
        return ClusterSharding.get(system).entityRefFor(VEHICLE_ENTITY_KEY, encode(tenantId, vin));
    }

    static String encode(String tenantId, String vin) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(tenantId.getBytes(StandardCharsets.UTF_8)) + "."
            + encoder.encodeToString(vin.getBytes(StandardCharsets.UTF_8));
    }

    static VehicleIdentity decode(String entityId) {
        String[] parts = entityId.split("\\.", -1);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid vehicle entity identity");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        return new VehicleIdentity(
            new String(decoder.decode(parts[0]), StandardCharsets.UTF_8),
            new String(decoder.decode(parts[1]), StandardCharsets.UTF_8));
    }

    record VehicleIdentity(String tenantId, String vin) {}
}
