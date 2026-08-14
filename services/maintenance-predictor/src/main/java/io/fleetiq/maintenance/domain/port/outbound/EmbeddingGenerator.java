package io.fleetiq.maintenance.domain.port.outbound;

import io.fleetiq.maintenance.domain.model.GeneratedEmbedding;
import io.smallrye.mutiny.Uni;

/** Generates local semantic vectors without exposing a model library to the domain. */
public interface EmbeddingGenerator {
    Uni<GeneratedEmbedding> generate(String content);
}
