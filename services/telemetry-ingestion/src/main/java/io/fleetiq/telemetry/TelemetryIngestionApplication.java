package io.fleetiq.telemetry;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class TelemetryIngestionApplication {

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
