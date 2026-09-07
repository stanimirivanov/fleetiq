package io.fleetiq.maintenance.adapter.outbound.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.fleetiq.maintenance.domain.model.AnomalyAssessment;
import io.fleetiq.maintenance.domain.model.RecommendationAdvice;
import io.fleetiq.maintenance.domain.model.SimilarIncident;
import io.fleetiq.maintenance.domain.port.outbound.RecommendationModel;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.List;

/**
 * Local Ollama adapter configured for deterministic, structured recommendations.
 * The blocking model call runs on a worker pool, prompt evidence is bounded, and JSON output is
 * parsed into the narrow advice type before it reaches the prediction engine.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class OllamaRecommendationModel implements RecommendationModel {

    private static final String SYSTEM_PROMPT = """
        You are a fleet maintenance assistant. Return one JSON object with exactly:
        {"recommendation":"concise action","evidenceIds":["allowed id"]}.
        Use only evidence IDs present in the request. Never alter the supplied
        component, severity, probability, or estimated failure horizon.
        """;

    private final ObjectMapper objectMapper;

    @ConfigProperty(name = "fleetiq.ai.ollama.base-url", defaultValue = "http://localhost:11434")
    String baseUrl;

    @ConfigProperty(name = "fleetiq.ai.ollama.model", defaultValue = "qwen2.5:1.5b")
    String modelName;

    private volatile OllamaChatModel model;

    @Override
    public Uni<RecommendationAdvice> recommend(String vin, AnomalyAssessment assessment,
                                                List<SimilarIncident> similarIncidents) {
        String prompt = prompt(vin, assessment, similarIncidents);
        return Uni.createFrom().item(() -> {
                String json = model().chat(
                    SystemMessage.from(SYSTEM_PROMPT), UserMessage.from(prompt))
                    .aiMessage().text();
                try {
                    return objectMapper.readValue(json, RecommendationAdvice.class);
                } catch (java.io.IOException e) {
                    throw new IllegalArgumentException("Recommendation model returned invalid JSON", e);
                }
            })
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private String prompt(String vin, AnomalyAssessment assessment,
                          List<SimilarIncident> similarIncidents) {
        var evidence = new StringBuilder();
        assessment.evidenceIds().forEach(id -> evidence.append("- ").append(id)
            .append(": current telemetry assessment\n"));
        similarIncidents.forEach(incident -> {
            if (incident.evidenceId() != null) {
                evidence.append("- ").append(incident.evidenceId()).append(": ")
                    .append(sanitize(incident.content())).append('\n');
            }
        });
        return """
            VIN: %s
            Component: %s
            Severity: %s
            Failure probability: %.3f
            Estimated days until failure: %d
            Deterministic recommendation: %s
            Authorized evidence:
            %s
            """.formatted(vin, assessment.component(), assessment.severity(),
            assessment.failureProbability(), assessment.estimatedDaysUntilFailure(),
            assessment.recommendation(), evidence);
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        String sanitized = value.replaceAll("[\\r\\n]+", " ");
        return sanitized.substring(0, Math.min(sanitized.length(), 1_000));
    }

    private OllamaChatModel model() {
        var current = model;
        if (current == null) {
            synchronized (this) {
                current = model;
                if (current == null) {
                    var builder = OllamaChatModel.builder();
                    builder.baseUrl(baseUrl).modelName(modelName).responseFormat(ResponseFormat.JSON)
                        .temperature(0.0).seed(42).numCtx(4_096).numPredict(300)
                        .timeout(Duration.ofSeconds(60));
                    builder.maxRetries(1);
                    current = builder.build();
                    model = current;
                }
            }
        }
        return current;
    }
}
