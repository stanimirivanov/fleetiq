package io.fleetiq.maintenance.adapter.outbound.persistence;

import io.fleetiq.maintenance.domain.model.GeneratedEmbedding;
import io.fleetiq.maintenance.domain.model.SimilarIncident;
import io.fleetiq.maintenance.domain.model.TelemetryEmbedding;
import io.fleetiq.maintenance.domain.port.outbound.EmbeddingStore;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reactive pgvector adapter with tenant, VIN, and model identity in every query. */
@ApplicationScoped
@RequiredArgsConstructor
public class PgVectorEmbeddingStore implements EmbeddingStore {

    private static final String INSERT = """
        INSERT INTO telemetry_embeddings
            (id, tenant_id, vin, window_start, window_end, embedding, anomaly_score,
             model_name, model_version, dimensions, content, evidence_id, metadata)
        VALUES ($1, $2, $3, $4, $5, $6::vector, $7, $8, $9, $10, $11, $12, $13::jsonb)
        """;

    private static final String SIMILAR = """
        SELECT id, evidence_id, embedding <=> $3::vector AS distance, content, metadata
        FROM telemetry_embeddings
        WHERE tenant_id = $1 AND vin = $2
          AND model_name = $4 AND model_version = $5 AND dimensions = $6
          AND id <> $7
        ORDER BY embedding <=> $3::vector, id
        LIMIT $8
        """;

    private final PgPool client;

    @Override
    public Uni<TelemetryEmbedding> save(String tenantId, TelemetryEmbedding value) {
        validateTenant(tenantId);
        GeneratedEmbedding embedding = value.embedding();
        Tuple parameters = Tuple.tuple()
            .addUUID(value.id()).addString(tenantId).addString(value.vin())
            .addOffsetDateTime(value.windowStart().atOffset(java.time.ZoneOffset.UTC))
            .addOffsetDateTime(value.windowEnd().atOffset(java.time.ZoneOffset.UTC))
            .addString(vector(embedding.values())).addDouble(value.anomalyScore())
            .addString(embedding.modelName()).addString(embedding.modelVersion())
            .addInteger(embedding.dimensions()).addString(value.content())
            .addValue(value.evidenceId()).addString(JsonObject.mapFrom(value.metadata()).encode());
        return client.preparedQuery(INSERT).execute(parameters)
            .replaceWith(value);
    }

    @Override
    public Uni<List<SimilarIncident>> findSimilar(String tenantId, String vin,
                                                   GeneratedEmbedding query, int limit,
                                                   UUID excludingId) {
        validateTenant(tenantId);
        if (limit < 1 || limit > 100) {
            return Uni.createFrom().failure(new IllegalArgumentException("Similarity limit must be between 1 and 100"));
        }
        Tuple parameters = Tuple.tuple().addString(tenantId).addString(vin)
            .addString(vector(query.values())).addString(query.modelName())
            .addString(query.modelVersion()).addInteger(query.dimensions())
            .addUUID(excludingId).addInteger(limit);
        return client.preparedQuery(SIMILAR).execute(parameters)
            .map(rows -> {
                var incidents = new ArrayList<SimilarIncident>();
                rows.forEach(row -> incidents.add(new SimilarIncident(
                    row.getUUID("id"),
                    row.getUUID("evidence_id"),
                    row.getDouble("distance"),
                    row.getString("content"),
                    metadata(row.getValue("metadata")))));
                return List.copyOf(incidents);
            });
    }

    private static String vector(List<Float> values) {
        return values.stream().map(String::valueOf)
            .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadata(Object value) {
        if (value instanceof JsonObject json) return json.getMap();
        if (value instanceof String json) return new JsonObject(json).getMap();
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        return Map.of();
    }

    private static void validateTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
    }
}
