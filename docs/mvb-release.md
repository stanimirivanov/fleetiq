# Minimum Viable Baseline Release

## Purpose

The FleetIQ MVB is a locally reproducible, educational reference system. It must
demonstrate the important architectural boundaries with executable code and tests;
it is not presented as a production-ready fleet platform. The broader
[implementation roadmap](implementation-roadmap.md) remains the capability and
hardening backlog.

## Release goals

- Demonstrate authenticated, tenant-isolated device and telemetry workflows.
- Persist telemetry and build topology from versioned asynchronous events.
- Serve authorized live telemetry with bounded resource use.
- Produce evidence-constrained local-AI maintenance advice without hosted APIs.
- Demonstrate durable tenant/VIN state behind the Pekko service boundary.
- Make the system understandable and repeatable for a reviewer on one workstation.

## Release acceptance criteria

### Build and contracts

- [x] A clean checkout passes `mvn clean verify` with the documented JDK and Maven.
- [x] Protobuf generates standard and Mutiny gRPC APIs and has a compatibility baseline.
- [x] Architecture tests protect the service hexagonal boundaries.
- [x] Root documentation versions, diagrams, links, and commands match the repository.

### Primary vertical slices

- [x] Authenticated simulator telemetry reaches ingestion through tenant-qualified MQTT.
- [x] Telemetry and position events commit atomically and persist in TimescaleDB.
- [x] Device and position outboxes feed tenant-safe topology projections.
- [x] A black-box test proves telemetry publication through persisted topology state.
- [x] A scripted local demo reproduces the primary journey with expected output.

### Security and streaming

- [x] gRPC roles, tenant propagation, database scoping, and MQTT ACL isolation are tested.
- [x] Device enrollment returns one-time development credentials without storing plaintext.
- [x] Streaming consumes real MQTT data and filters tenant identity before VIN selection.
- [x] Streaming applies tested per-principal subscription and slow-consumer limits.
- [x] Malformed/poison projection events have bounded retry and quarantine behavior.

### Predictive maintenance

- [x] Deterministic anomaly scoring precedes model inference.
- [x] Local versioned embeddings and tenant/VIN-scoped pgvector retrieval are tested.
- [x] Local structured RAG rejects citations outside the authorized evidence set.
- [x] High-confidence recommendations use a transactional outbox and versioned event.
- [x] An integration scenario proves anomalous telemetry to persisted, cited recommendation.

### Pekko state boundary

- [x] Protobuf/gRPC hides actor APIs and tenant/VIN identity is enforced.
- [x] Vehicle state, commands, events, and invariants are explicit and tested.
- [x] Event-sourced state recovers from the JDBC journal in an integration test.
- [x] Duplicate command handling is idempotent.

## Deferred production-hardening backlog

The following roadmap work is intentionally not an MVB release blocker:

- Production credential provider selection, certificate lifecycle, RLS, and security auditing.
- Scheduled fleet-wide prediction, feedback/calibration metrics, and automated model evaluation.
- Advanced impact analysis, route deviation, replayable device-command delivery, and multi-node chaos tests.
- Shared test-infrastructure optimization, coverage thresholds, load/capacity baselines, and formal SLOs.
- HA/scale-to-zero demonstrations, backup/restore exercises, production secret management,
  signed images, SBOM/provenance, and automated rollout/rollback.

These are still valuable future milestones. Deferring them keeps the MVB claim
honest and prevents production-readiness language from outrunning executable proof.

## Completion rule

The MVB is complete only when every unchecked release acceptance criterion above
is implemented or deliberately removed from scope in an ADR. The final verification
record must include the commands run, passing test counts, and any environment-only
limitations observed on the release workstation.
