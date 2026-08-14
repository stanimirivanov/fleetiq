package io.fleetiq.maintenance.domain.model;

import java.util.List;

/** Vector plus the immutable model identity required to interpret it correctly. */
public record GeneratedEmbedding(
    String modelName,
    String modelVersion,
    int dimensions,
    List<Float> values
) {
    public GeneratedEmbedding {
        values = List.copyOf(values);
        if (values.size() != dimensions) {
            throw new IllegalArgumentException("Embedding dimensions do not match vector length");
        }
    }
}
