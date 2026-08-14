# Maintenance Predictor

Coordinates maintenance prediction, stores predictions and maintenance evidence, and
exposes the capability through an authenticated gRPC API.

- Inbound boundaries: gRPC and scheduled prediction work.
- Persistence: PostgreSQL JSONB, managed by Flyway.
- Tenant isolation: evidence, predictions, embeddings, repository queries, and gRPC
  operations are scoped by tenant.
- Prediction adapter: `PredictionEngine` isolates deterministic generation today
  and future LangChain4j enrichment from application orchestration.
- Telemetry evidence: an authenticated gRPC client obtains tenant-scoped windows
  from Telemetry Ingestion through the `TelemetryWindowSource` port.
- Deterministic baseline: explainable engine-temperature and battery-voltage
  thresholds produce severity, probability, recommendation, and evidence citations
  before any optional RAG enrichment.
- Similarity retrieval: local 384-dimensional ONNX embeddings are stored with model
  identity and queried through tenant-and-VIN-scoped pgvector cosine distance.
- Verify: `mvn -pl services/maintenance-predictor -am verify`.

The deterministic prediction and embedding-similarity slices are implemented;
LangChain4j RAG enrichment is still pending. Treat this module as an evolving
boundary rather than a production ML workflow. Scheduled prediction work must
enumerate tenants explicitly when implemented; a scheduler has no authenticated
request tenant.
