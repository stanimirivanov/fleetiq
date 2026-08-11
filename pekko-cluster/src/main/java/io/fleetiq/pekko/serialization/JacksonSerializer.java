package io.fleetiq.pekko.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Provides a configured Jackson ObjectMapper for Pekko serialization.
 * Referenced from application.conf via:
 * pekko.serialization.jackson.jackson-object-mapper-factory
 */
public final class JacksonSerializer {

    private JacksonSerializer() {}

    /**
     * Creates the ObjectMapper used by Pekko's built-in Jackson serializer.
     */
    public static ObjectMapper create() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule());
    }
}
