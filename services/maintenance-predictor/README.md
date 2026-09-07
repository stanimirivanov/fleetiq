# Maintenance Predictor

Coordinates maintenance prediction, stores predictions and maintenance evidence, and
exposes the capability through an authenticated gRPC API.

- Inbound boundaries: gRPC and scheduled prediction work.
- Persistence: PostgreSQL JSONB, managed by Flyway.
- Tenant isolation: evidence, predictions, embeddings, repository queries, and gRPC
  operations are scoped by tenant.
- Prediction adapters: `PredictionEngine` isolates orchestration from inference,
  while `RecommendationModel` isolates local chat-model access from validation.
- Telemetry evidence: an authenticated gRPC client obtains tenant-scoped windows
  from Telemetry Ingestion through the `TelemetryWindowSource` port.
- Deterministic baseline: explainable engine-temperature and battery-voltage
  thresholds produce severity, probability, recommendation, and evidence citations
  before any optional RAG enrichment.
- Similarity retrieval: local 384-dimensional ONNX embeddings are stored with model
  identity and queried through tenant-and-VIN-scoped pgvector cosine distance.
- Recommendation enrichment: local `qwen2.5:1.5b` inference through Ollama returns
  JSON advice. `RagPredictionEngine` preserves deterministic risk fields and rejects
  citations outside the tenant-scoped evidence supplied to the model.
- Event publication: predictions at or above the configurable confidence threshold
  are committed with a protobuf event in a transactional outbox. The reactive relay
  publishes to `fleetiq/events/maintenance-recommendations` with at-least-once
  delivery; consumers must deduplicate by `event_id`.
- CI behavior: unit tests inject a deterministic recommendation-model fake; no
  Ollama process, hosted AI API, or model download is required.
- Local model: `docker compose -f infra/docker-compose/docker-compose.yml up -d ollama ollama-model`
  starts Ollama and pulls the documented model once into a named volume.
- Verify: `mvn -pl services/maintenance-predictor -am verify`.

The deterministic prediction, embedding-similarity, and local RAG slices are
implemented. Treat this module as an evolving advisory workflow rather than a
production safety system. Scheduled prediction work must enumerate tenants
explicitly when implemented; a scheduler has no authenticated request tenant.
