# FleetIQ Implementation Roadmap

## Purpose and maintenance

This is the living implementation plan for FleetIQ. It records completed work,
orders future work by dependency, and gives each milestone an observable definition
of done. `[x]` means implemented and verified in the repository; `[ ]` means not
implemented or not yet proven.

Update this document in the same change as each implementation step. Check an item
only when code, an automated test, or a documented operational exercise proves it.
Create an ADR when a step changes a public contract, security boundary, data owner,
or deployment model.

## Phase 1 — Buildable reactive foundation

### Goals

Provide a reproducible multi-module build, generated Mutiny gRPC APIs, non-blocking
service implementations, and verified database migrations.

### Acceptance criteria

- [x] Protobuf generates Java messages, standard gRPC classes, and Mutiny stubs.
- [x] Services consume generated contracts without copying transport models.
- [x] Telemetry, Device Registry, and Fleet Topology use reactive persistence.
- [x] Flyway uses JDBC while runtime persistence remains reactive.
- [x] Migration tests use the required PostgreSQL extensions.
- [ ] A clean environment succeeds with `mvn clean verify`.
- [ ] CI enforces the documented JDK and Maven versions.

### Tasks

- [x] Correct protobuf generation and source attachment.
- [x] Align datasource and Flyway configuration.
- [x] Add migration tests for TimescaleDB, AGE/PostGIS, and pgvector.
- [ ] Update dependencies that emit Quarkus relocation warnings.
- [ ] Add build-toolchain validation.

## Phase 2 — Event-driven vertical slices

### Goals

Prove real flows across module boundaries and build topology asynchronously without
synchronous query-time fan-out.

### Acceptance criteria

- [x] Simulator telemetry reaches Telemetry Ingestion through tenant-qualified MQTT.
- [x] Valid telemetry persists in TimescaleDB.
- [x] Device and position changes use transactional outboxes.
- [x] Relays safely claim events across replicas with at-least-once delivery.
- [x] Topology projections are idempotent and reject stale events.
- [x] AGE vertices, relationships, traversal, and proximity queries are implemented.
- [ ] A black-box test proves simulator → broker → ingestion → database → topology.
- [ ] Poison events have bounded retries and a quarantine path.
- [ ] Historical telemetry supports distance and time-bucket aggregations required by analytics.
- [ ] Advanced topology proves downstream impact analysis and route-deviation detection.

### Tasks

- [x] Implement MQTT ingestion and TimescaleDB persistence.
- [x] Define additive projection contracts and outbox relays.
- [x] Implement relational projection guards and AGE synchronization.
- [ ] Standardize event IDs, correlation IDs, and schema-version metadata.
- [ ] Add retry/backoff and dead-letter handling.
- [ ] Add the cross-service black-box test.
- [ ] Add TimescaleDB aggregate queries for total distance and time buckets.
- [ ] Add topology impact-analysis and route-deviation use cases with tests.
- [ ] Add a 1,000-vehicle/1 Hz load scenario and record the baseline.

## Phase 3 — Hexagonal architecture and domain contracts

### Goals

Keep business decisions independent of infrastructure, make expected outcomes
explicit, and keep module boundaries understandable and enforceable.

### Acceptance criteria

- [x] Architecture tests enforce inward dependency direction in every service.
- [x] Domain packages do not import protobuf, MQTT, entities, or adapters.
- [x] Expected outcomes use `Optional` or sealed result types.
- [x] Unexpected infrastructure errors remain reactive failures.
- [x] Repository contracts exercise real adapters.
- [x] Deployable/shared modules have focused READMEs and selective Javadoc.
- [ ] Architecture rules also cover the simulator and Pekko module.

### Tasks

- [x] Publish architecture and documentation policies.
- [x] Add ArchUnit, domain, mapper, and repository-contract tests.
- [x] Remove exception-driven expected repository outcomes.
- [ ] Add checks for blocking work in reactive adapters.
- [ ] Extract shared transport error mapping only when repetition warrants it.

## Phase 4 — API compatibility discipline

### Goals

Treat protobuf as a versioned public contract and keep transport/domain mapping explicit.

### Acceptance criteria

- [x] Device Registry gRPC maps validated application commands and explicit outcomes.
- [x] Descriptor compatibility tests protect existing protobuf contracts.
- [x] Tenant identity comes from authenticated context, not request payloads.
- [ ] Every RPC has authentication, authorization, validation, and status tests.
- [ ] Deprecation/versioning policy includes an example migration.
- [ ] CI publishes a discoverable contract catalog.

### Tasks

- [x] Add Device Registry mapper and adapter tests.
- [x] Check in and verify the protobuf descriptor baseline.
- [x] Add `EnrollDevice` without changing existing field numbers.
- [ ] Audit all RPCs against the boundary test matrix.
- [ ] Add Buf lint/breaking checks or an equivalent CI gate.
- [ ] Document compatibility and deprecation windows.

## Phase 5 — Tenant isolation and device identity

### Goals

Make tenant identity explicit across APIs, events, data, streams, actors, and MQTT;
apply least privilege and isolate credential lifecycle from ordinary device metadata.

### Acceptance criteria

- [x] APIs establish `CurrentTenant` and enforce roles.
- [x] Device, telemetry, topology, maintenance, streaming, and Pekko keys are tenant-scoped.
- [x] MQTT rejects anonymous clients and restricts backend service topics.
- [x] `<tenant>/<vin>` device principals can publish only their exact telemetry topic.
- [x] Integration tests prove anonymous, cross-tenant, cross-VIN, and service denial.
- [x] Operator-only enrollment returns a one-time credential through an outbound port.
- [x] The educational credential provider stores only salted verifiers and is disabled in production.
- [ ] A production provider supports provisioning, rotation, and revocation.
- [ ] MQTT uses TLS and strong per-device authentication in production.
- [ ] PostgreSQL row-level security protects tenant-owned tables.
- [ ] Security audit events cover credential and administrative actions.

### Tasks

- [x] Scope schemas, repositories, commands, projections, streams, and actors by tenant.
- [x] Configure Mosquitto authentication, ACLs, and security integration tests.
- [x] Introduce `DeviceCredentialProvisioner` and explicit enrollment outcomes.
- [ ] Write an ADR selecting Vault, Mosquitto Dynamic Security, or a managed broker.
- [ ] Implement credential provisioning, rotation, and immediate revocation.
- [ ] Revoke credentials when devices are decommissioned.
- [ ] Configure TLS, trust, renewal, and expiry monitoring.
- [ ] Add RLS policies and cross-tenant SQL denial tests.

## Phase 6 — Predictive maintenance with AI

### Goals

Build an evidence-based maintenance workflow: derive statistical anomalies from
telemetry windows, retrieve similar incidents through pgvector, and use a local
LangChain4j model to produce structured recommendations with cited evidence.
AI output is advisory, tenant-scoped, reproducible, and never silently substituted
for deterministic safety decisions.

### Acceptance criteria

- [x] Maintenance evidence and predictions have tenant-scoped JSONB persistence.
- [x] Flyway creates and verifies tenant-scoped pgvector embedding storage.
- [x] Authenticated gRPC boundaries record evidence and query prediction history.
- [x] Telemetry windows are obtained through an explicit outbound port/API without cross-database access.
- [x] Statistical anomaly detection is deterministic and unit-tested before LLM use.
- [x] Local embedding generation stores model name/version and vector dimensions.
- [x] Similar-incident retrieval is tenant/VIN scoped and uses pgvector distance ordering.
- [ ] RAG prompts include only authorized evidence and return a validated structured result.
- [x] Predictions persist component, probability, severity, recommendation, and evidence citations.
- [ ] High-confidence recommendations publish an event for Pekko/alert handling.
- [ ] Actual outcomes link to predictions and produce accuracy/calibration metrics.
- [ ] The AI path has deterministic fakes for CI and never requires a hosted external API.

### Tasks

- [x] Establish the Maintenance Predictor hexagonal boundary, JSONB entities, pgvector migration, and tenant isolation.
- [x] Replace the placeholder `LangChain4jPredictionEngine` with an outbound `PredictionEngine` port.
- [x] Define a telemetry-window outbound port and authenticated telemetry API.
- [x] Define the embedding-store outbound port.
- [x] Implement statistical baselines and anomaly scoring as deterministic domain logic.
- [x] Select and document the local embedding model, dimensions, resource needs, and license.
- [ ] Select and document the local chat model, resource needs, structured-output support, and license.
- [x] Implement embedding persistence and tenant-safe similarity search.
- [ ] Implement structured LangChain4j RAG generation with schema validation and evidence citations.
- [ ] Make scheduled work enumerate tenants explicitly and protect it with a lease/claim strategy.
- [ ] Publish maintenance recommendation events and consume them behind the Pekko boundary.
- [ ] Add feedback capture, accuracy metrics, and an AI evaluation dataset.
- [ ] Add an E2E demo: anomalous telemetry → similar incidents → cited recommendation.

## Phase 7 — Pekko stateful processing and command control

### Goals

Keep Pekko behind a stable boundary and prove durable, recoverable per-vehicle
behavior. Then add reliable platform-to-device commands with timeout, retry,
response correlation, auditing, and failure escalation.

### Acceptance criteria

- [x] A protobuf/gRPC boundary hides actor APIs.
- [x] Shard identity combines tenant and VIN and actors validate ownership.
- [ ] Vehicle actors are event-sourced with stable persistence IDs.
- [ ] Journal/snapshot migrations and recovery are integration-tested.
- [ ] Duplicate commands and events are idempotent.
- [ ] Multi-node tests prove sharding, passivation, relocation, and shutdown.
- [ ] Supervision, dead letters, and readiness are observable.
- [ ] gRPC commands route through sharding to the tenant/VIN actor.
- [ ] Commands publish to tenant-qualified MQTT topics and responses correlate by command ID.
- [ ] Timeout/retry policy is configurable by command type and survives actor recovery.
- [ ] Exhausted commands enter a replayable dead-letter/quarantine workflow.
- [ ] Command requests, attempts, responses, and terminal outcomes form an audit trail.

### Tasks

- [x] Define the service boundary and tenant-aware entity identity.
- [ ] Define vehicle state, commands, events, and invariants.
- [ ] Implement event sourcing, snapshots, retention, and schema evolution.
- [ ] Add recovery, idempotency, and multi-node tests.
- [ ] Connect telemetry with backpressure and retry policy.
- [ ] Define command, acknowledgement, response, timeout, and audit event contracts.
- [ ] Add tenant-qualified command/response ACLs and simulator command handling.
- [ ] Implement durable retry timers, response correlation, and idempotency.
- [ ] Add supervision/recovery and manual replay demonstrations.

## Phase 8 — Streaming delivery

### Goals

Deliver live data from real sources with authorization, bounded resources,
backpressure, and clear reconnect semantics.

### Acceptance criteria

- [x] Streaming Hub consumes real MQTT rather than an in-memory subscriber registry.
- [x] Streams filter authenticated tenant before VIN selection.
- [x] Throttling is reactive and non-blocking.
- [ ] Slow-consumer buffer/drop/disconnect policy is defined and tested.
- [ ] Reconnect and delivery semantics are documented.
- [ ] Limits prevent subscription or memory exhaustion.
- [ ] Client integration tests prove authorization and cancellation cleanup.

### Tasks

- [x] Add the event-source port, MQTT adapter, and tenant-aware tests.
- [ ] Record the backpressure policy in an ADR.
- [ ] Add per-principal concurrency and subscription limits.
- [ ] Add replay/resume only if product requirements demand it.
- [ ] Test cancellation, slow consumers, and broker interruption end to end.

## Phase 9 — Test architecture and quality gates

### Goals

Create a deterministic test pyramid and make migrations, compatibility, security,
and critical vertical slices required CI gates.

### Acceptance criteria

- [x] The testing strategy defines unit, contract, integration, security, and E2E layers.
- [x] Testcontainers cover PostgreSQL extensions and secured MQTT.
- [x] Architecture, domain, mapper, repository, migration, and compatibility tests exist.
- [ ] Container tests consistently use `*IT` and Maven Failsafe.
- [ ] A shared integration harness removes duplicated infrastructure setup.
- [ ] Tests are independent of reusable-container residue and execution order.
- [ ] CI runs unit tests and required integration/security suites.
- [ ] Coverage gates exclude generated code and emphasize behavior.

### Tasks

- [x] Add the initial layered suites and MQTT authorization contract.
- [ ] Move container-backed tests to Failsafe.
- [ ] Share TimescaleDB, AGE/PostGIS, pgvector, Mosquitto, and Keycloak resources.
- [ ] Add deterministic cleanup and unique test identities.
- [ ] Add E2E enrollment, telemetry, projection, and streaming scenarios.
- [ ] Publish reports and enforce required CI checks.

## Phase 10 — Resilience and operational correctness

### Goals

Make failures recoverable and ensure health signals and runbooks represent actual behavior.

### Acceptance criteria

- [ ] Remote boundaries have intentional timeout, retry, and idempotency policies.
- [ ] Retries are bounded and use backoff with jitter.
- [ ] Health checks distinguish liveness from dependency readiness.
- [ ] Outbox lag, dead letters, broker disconnects, pool pressure, and actor failures are measurable.
- [ ] Graceful shutdown drains work and safely releases leases.
- [ ] Backup/restore and disaster-recovery procedures are exercised.
- [ ] High-impact incidents have operational runbooks.
- [ ] A slowed dependency demonstrates the selected timeout/circuit-breaker behavior.
- [ ] Knative scale-to-zero and cold-start behavior are measured for eligible services.
- [ ] A killed Pekko node demonstrates shard rebalance and journal recovery time.
- [ ] Database interruption demonstrates buffering or explicit backpressure without silent loss.
- [ ] Rolling deployment demonstrates no MQTT loss and documented stream reconnection.

### Tasks

- [ ] Inventory failure modes and retry ownership.
- [ ] Add timeouts, idempotency keys, and justified circuit breakers.
- [ ] Add outbox metrics and quarantine tooling.
- [ ] Implement dependency readiness and graceful shutdown.
- [ ] Write broker, database, credential-compromise, and projection-rebuild runbooks.
- [ ] Perform and document backup/restore.
- [ ] Automate failure demonstrations for dependency slowdown, node loss, database outage, and rolling deployment.

## Phase 11 — Observability and performance

### Goals

Provide end-to-end correlation and service-level signals, then validate capacity
using representative workloads.

### Acceptance criteria

- [ ] Trace/correlation context crosses gRPC, MQTT, outbox, and actor boundaries.
- [ ] Logs are structured and redact credentials and sensitive data.
- [ ] RED and USE metrics cover APIs, messaging, and critical resources.
- [ ] Dashboards cover ingestion, latency, failures, outbox lag, streams, and actors.
- [ ] Critical journeys have documented SLOs and actionable alerts.
- [ ] Load tests establish reproducible baselines and scaling evidence.

### Tasks

- [ ] Standardize correlation metadata and OpenTelemetry propagation.
- [ ] Add secret-redaction and structured-logging tests.
- [ ] Add service metrics and Grafana dashboards.
- [ ] Define SLOs and alerts linked to runbooks.
- [ ] Add ingestion/streaming load tests and record baselines.

## Phase 12 — Deployment and production readiness

### Goals

Make environments reproducible, secrets external, artifacts traceable, and
rollouts safe while preserving service/platform ownership.

### Acceptance criteria

- [x] Kubernetes workload/platform ownership is documented.
- [x] Workload directories are independently composable Kustomize bases.
- [x] Docker Compose supplies local infrastructure.
- [x] ArgoCD and image-update manifests provide a GitOps deployment baseline.
- [ ] All bases and overlays render and validate in CI.
- [ ] Production secrets are external to Git.
- [ ] Images are digest-pinned, scanned, signed, and have SBOMs.
- [ ] Data and event changes support backward-compatible rollout order.
- [ ] Smoke tests and rollback procedures are automated.
- [ ] Production review has no critical security or data-loss findings.

### Tasks

- [x] Separate service and platform ownership structurally.
- [ ] Add Kustomize schema/policy validation.
- [ ] Replace placeholder Secrets with an external/encrypted workflow.
- [ ] Add image pinning, scanning, signing, SBOMs, and provenance.
- [ ] Automate staged rollout, smoke tests, and rollback.
- [ ] Run threat modeling and production-readiness review.

## Phase 13 — Portfolio and educational completeness

### Goals

Make FleetIQ easy to understand, run, evaluate, and extend without documentation drift.

### Acceptance criteria

- [x] Root and module READMEs explain responsibilities and boundaries.
- [x] Architecture, testing, deployment, and documentation policies exist.
- [x] This roadmap tracks completed and future implementation.
- [ ] Diagrams match implemented communication and deployment paths.
- [ ] ADRs explain consequential choices and alternatives.
- [ ] A reproducible demo script exercises the primary journey.
- [ ] Documentation links and commands are checked automatically.

### Tasks

- [x] Add selective educational Javadoc and module documentation.
- [x] Add and index this roadmap.
- [ ] Correct stale versions, URLs, and arrows in the root README.
- [ ] Add ADRs for credentials, event transport, streaming, and actor persistence.
- [ ] Add a scripted demo with expected output.
- [ ] Add Markdown link and command validation to CI.

## Immediate implementation order

1. Add a local-model RAG adapter with schema-validated output and deterministic CI fakes.
2. Select and implement the production credential provider with rotation and revocation.
3. Move container-backed tests to Failsafe and introduce a shared integration harness.
4. Add poison-event retry limits, quarantine, and operational metrics.
5. Implement Pekko event sourcing/recovery, then reliable MQTT command delivery.
6. Define and test streaming backpressure and subscription limits.
7. Add enrollment, telemetry, topology, maintenance, and streaming E2E scenarios.
8. Complete observability, resilience demonstrations, and production deployment gates.
