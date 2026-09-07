package io.fleetiq.maintenance.domain.port.outbound;

import io.fleetiq.maintenance.domain.model.GeneratedEmbedding;
import io.fleetiq.maintenance.domain.model.SimilarIncident;
import io.fleetiq.maintenance.domain.model.TelemetryEmbedding;
import io.smallrye.mutiny.Uni;

import java.util.List;

/** Tenant-scoped persistence and cosine-similarity boundary for derived evidence. */
public interface EmbeddingStore {
    /** Persists the vector together with the model identity required to interpret it later. */
    Uni<TelemetryEmbedding> save(String tenantId, TelemetryEmbedding embedding);

    /**
     * Returns the closest compatible vectors for the tenant and VIN, excluding the current vector
     * when {@code excludingId} is supplied.
     */
    Uni<List<SimilarIncident>> findSimilar(
        String tenantId,
        String vin,
        GeneratedEmbedding query,
        int limit,
        java.util.UUID excludingId);
}
