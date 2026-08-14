package io.fleetiq.maintenance.domain.model;

import java.util.Map;
import java.util.UUID;

/** Authorized evidence returned in ascending cosine-distance order. */
public record SimilarIncident(
    UUID embeddingId,
    UUID evidenceId,
    double distance,
    String content,
    Map<String, Object> metadata
) {}
