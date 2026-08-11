package io.fleetiq.pekko;

import io.fleetiq.pekko.actor.FleetGuardian;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Behavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FleetClusterApp {

    private static final Logger log = LoggerFactory.getLogger(FleetClusterApp.class);

    public static void main(String[] args) {
        log.info("Starting FleetIQ Pekko Cluster...");
        Behavior<FleetGuardian.Command> rootBehavior = FleetGuardian.create();
        ActorSystem<FleetGuardian.Command> system = ActorSystem.create(rootBehavior, "fleetiq-cluster");
        log.info("Pekko Cluster started. Press Ctrl+C to stop.");
    }
}
