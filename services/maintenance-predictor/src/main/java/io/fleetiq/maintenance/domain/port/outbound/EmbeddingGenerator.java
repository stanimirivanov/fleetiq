package io.fleetiq.maintenance.domain.port.outbound;

import io.fleetiq.maintenance.domain.model.GeneratedEmbedding;
import io.smallrye.mutiny.Uni;

/** Generates local semantic vectors without exposing a model library to the domain. */
public interface EmbeddingGenerator {
    /** Generates a vector and the model metadata needed for compatible similarity searches. */
    Uni<GeneratedEmbedding> generate(String content);
}
