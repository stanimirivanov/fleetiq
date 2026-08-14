package io.fleetiq.maintenance.adapter.outbound.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalOnnxEmbeddingGeneratorTest {

    @Test
    void generatesVersionedLocalEmbeddingWithExpectedDimensions() {
        var generator = new LocalOnnxEmbeddingGenerator();
        var first = generator.generate("engine cooling temperature anomaly").await().indefinitely();
        var second = generator.generate("engine cooling temperature anomaly").await().indefinitely();

        assertEquals(LocalOnnxEmbeddingGenerator.MODEL_NAME, first.modelName());
        assertEquals(LocalOnnxEmbeddingGenerator.MODEL_VERSION, first.modelVersion());
        assertEquals(384, first.dimensions());
        assertEquals(384, first.values().size());
        assertEquals(first.values(), second.values());
    }
}
