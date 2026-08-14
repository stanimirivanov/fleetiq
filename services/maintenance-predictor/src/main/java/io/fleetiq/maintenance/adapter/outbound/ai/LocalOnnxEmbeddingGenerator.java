package io.fleetiq.maintenance.adapter.outbound.ai;

import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import io.fleetiq.maintenance.domain.model.GeneratedEmbedding;
import io.fleetiq.maintenance.domain.port.outbound.EmbeddingGenerator;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;

/** Runs the 384-dimensional all-MiniLM-L6-v2 model locally on a worker thread. */
@ApplicationScoped
public class LocalOnnxEmbeddingGenerator implements EmbeddingGenerator {

    public static final String MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2";
    public static final String MODEL_VERSION = "langchain4j-1.18.1-beta28";
    public static final int DIMENSIONS = 384;

    private volatile AllMiniLmL6V2EmbeddingModel model;

    @Override
    public Uni<GeneratedEmbedding> generate(String content) {
        if (content == null || content.isBlank()) {
            return Uni.createFrom().failure(new IllegalArgumentException("Embedding content is required"));
        }
        return Uni.createFrom().item(() -> {
                float[] vector = model().embed(content).content().vector();
                var values = new ArrayList<Float>(vector.length);
                for (float value : vector) values.add(value);
                return new GeneratedEmbedding(MODEL_NAME, MODEL_VERSION, DIMENSIONS, values);
            })
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private AllMiniLmL6V2EmbeddingModel model() {
        var current = model;
        if (current == null) {
            synchronized (this) {
                current = model;
                if (current == null) {
                    current = new AllMiniLmL6V2EmbeddingModel();
                    model = current;
                }
            }
        }
        return current;
    }
}
