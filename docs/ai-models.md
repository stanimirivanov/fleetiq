# AI Model Baseline

FleetIQ keeps model libraries behind domain ports so model selection does not
change application orchestration or persistence contracts. Hosted AI APIs are not
required for development or CI.

## Telemetry embeddings

- Model: [`sentence-transformers/all-MiniLM-L6-v2`](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)
  through [LangChain4j's in-process ONNX adapter](https://docs.langchain4j.dev/integrations/embedding-models/in-process/).
- Adapter version: `langchain4j-1.18.1-beta28`.
- Output: 384-dimensional semantic vectors.
- Runtime: local CPU inference; the model artifact adds approximately 83 MB to the
  Maven dependency set and requires no model server or API key.
- License: Apache-2.0. Model use and dataset suitability must be reassessed before
  using it for a commercial safety decision.
- Persistence: model name, adapter version, and dimensions accompany every vector.
  Similarity queries reject vectors produced by a different model contract.
- Tests: application tests use small deterministic vectors; a focused smoke test
  verifies the packaged ONNX model separately.

Changing the model or dimensions requires a Flyway migration and regeneration of
the derived `telemetry_embeddings` projection. Embeddings must never be silently
compared across model versions.

## Recommendation model

- Model: [`qwen2.5:1.5b`](https://ollama.com/library/qwen2.5:1.5b), served by
  Ollama and accessed through the LangChain4j Ollama adapter.
- Ollama model digest: `65ec06548149`. The tag currently resolves to a 986 MB
  Q4_K_M artifact; pin and revalidate the digest before a production release.
- License: Apache-2.0.
- Context: the model advertises a 32K context window, while FleetIQ deliberately
  limits requests to 4,096 tokens and responses to 300 tokens to bound local
  resource consumption.
- Runtime: local CPU inference at `http://localhost:11434`; no hosted API, API key,
  or external evidence source is required.
- Structured output: Ollama JSON response mode plus Jackson record deserialization.
  The application rejects blank or oversized recommendations and any citation that
  is not in the tenant-scoped evidence supplied to the prompt.
- Reproducibility: temperature `0`, seed `42`, a 60-second timeout, and one retry.
  These settings improve repeatability but do not make model inference a safety
  decision or guarantee identical output across runtimes and model revisions.
- Tests: application tests inject a deterministic `RecommendationModel` fake and
  therefore do not start Ollama or download a model. They verify both accepted and
  fabricated evidence citations.

The deterministic anomaly assessment remains authoritative for component,
probability, severity, and estimated failure horizon. The chat model may only
produce advisory text and select citations from the authorized evidence set.
