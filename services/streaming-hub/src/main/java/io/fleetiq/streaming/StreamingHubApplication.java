package io.fleetiq.streaming;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class StreamingHubApplication {
    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
