package io.fleetiq.pekko.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Factory for creating Jackson ObjectMapper instances.
 * Configured in application.conf via:
 * pekko.serialization.jackson.jackson-object-mapper-factory
 */
public class JacksonObjectMapperFactory {

    public JacksonObjectMapperFactory() {}

    public ObjectMapper newObjectMapper(String serializerIdentifier, com.typesafe.config.Config config) {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule());
    }
}
