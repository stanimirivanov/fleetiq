# Maintenance Predictor

Coordinates maintenance prediction, stores predictions and maintenance evidence, and
exposes the capability through an authenticated gRPC API.

- Inbound boundaries: gRPC and scheduled prediction work.
- Persistence: PostgreSQL JSONB, managed by Flyway.
- Prediction adapter: LangChain4j integration behind the application service.
- Verify: `mvn -pl services/maintenance-predictor -am verify`.

The prediction engine and repository are still less complete than the ingestion,
registry, and topology slices; treat this module as an evolving boundary rather than
a production ML workflow.
