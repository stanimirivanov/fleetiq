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

No chat model is approved yet. The next implementation step must record the local
model name and digest, resource requirements, license, context limit, structured
JSON support, and deterministic test substitute before enabling RAG generation.
