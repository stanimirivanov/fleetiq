package io.fleetiq.topology;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class TopologyApplication {
    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
