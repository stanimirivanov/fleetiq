# FleetIQ Architecture Baseline

## Scope

Each deployable service remains one Maven module. Hexagonal boundaries are enforced by packages and architecture tests. A layer is promoted to its own Maven module only when it has an independent release lifecycle or is a stable cross-service policy, such as `security-common` or `proto`.

## Package ownership

- `domain.model`: business state and invariants; no transport, persistence, Quarkus, or generated-proto types.
- `domain.port.inbound`: use-case interfaces only. Commands and results may remain nested here until an `application` package is introduced by a real orchestration need.
- `domain.port.outbound`: capabilities required by the application. Interfaces never expose adapter types.
- `domain.service`: use-case orchestration. It may depend on domain models, ports, Mutiny, and dependency-injection annotations, but never an adapter implementation.
- `adapter.inbound`: gRPC, MQTT, scheduler, and other driving adapters. It maps transport data and security context to use-case inputs.
- `adapter.outbound`: persistence, messaging, AI, and remote-client implementations. It maps domain values to infrastructure APIs.
- `bootstrap` or the module root: runtime startup and dependency wiring only.

## Dependency direction

```text
adapter.inbound -> domain.port.inbound -> domain.service -> domain.port.outbound <- adapter.outbound
```

Domain code must not import generated protobuf classes, gRPC, MQTT, persistence entities, or outbound adapter implementations. Inbound adapters must not bypass ports to call outbound adapters.

## Error policy

- Expected absence uses `Optional` or an explicit sealed application result.
- Expected business alternatives use sealed result types.
- Unexpected infrastructure failures remain failures of `Uni` or `Multi`.
- Infrastructure failures are logged once at a transport or process boundary.
- Mappers do not generate identifiers, timestamps, or business defaults.

## Tenant boundary

Authentication establishes `CurrentTenant` at inbound API boundaries. Tenant identity must become an explicit input to every tenant-owned use case and repository operation before multiple production tenants are enabled.

The migration must be performed as one compatibility change per service:

1. Add non-null `tenant_id` with an explicit backfill for existing development data.
2. Replace global keys and indexes with tenant-scoped keys, for example `(tenant_id, vin)`.
3. Add tenant identity to application commands and queries.
4. Require `TenantId` in outbound repository methods.
5. Prove cross-tenant denial with repository and API integration tests.
6. Optionally add PostgreSQL row-level security as defense in depth.

No adapter may silently substitute a default tenant. MQTT must first gain an authenticated device-to-tenant mapping; until then, multi-tenant mode is not production-ready.

### Rollout status

- Device Registry scopes gRPC commands, repository queries, database uniqueness, and
  projection events by authenticated tenant.
- Telemetry Ingestion scopes gRPC and MQTT commands, TimescaleDB rows and aggregates,
  repository queries, and position projection events by tenant. MQTT broker ACLs must
  still bind authenticated publishers to their tenant-qualified topic prefix.
- Fleet Topology scopes consumed projections, relational and AGE graph identity,
  relationships, traversal, proximity queries, and authenticated gRPC calls by tenant.
- Maintenance, streaming, and actor state still require the same
  explicit tenant migration before multi-tenant production use.

## API compatibility

- Protobuf field numbers are permanent and removed fields are reserved.
- Existing enum numeric values are never renumbered.
- RPC request/response types are not changed incompatibly; introduce a new RPC or versioned package instead.
- Transport DTOs never become domain models.

## Topology projection

Fleet Topology owns a local, eventually consistent Apache AGE read model. Device
Registry publishes device registration and status changes; Telemetry Ingestion
publishes normalized position changes. Fleet Topology consumes those contracts
and never performs synchronous query-time fan-out to the source services.

Projection events are additive protobuf contracts. Device Registry and Telemetry
Ingestion persist projection events transactionally with their source changes;
scheduled relays publish pending records to MQTT and remove them only after an
acknowledged send. Relays claim batches transactionally with `FOR UPDATE SKIP
LOCKED`, allowing multiple replicas to work without selecting the same record;
publication failure rolls the transaction back. Delivery is therefore at least once. Consumers must be
idempotent and reject stale updates using their event timestamps. Fleet
Topology applies this rule through timestamp-guarded relational projection
upserts; only accepted updates are synchronized to the corresponding Apache AGE
vertex in the same database transaction.

Relationship identity and traversal metadata are retained in a relational
projection with a matching AGE `connected_to` edge. Bounded traversal uses the
indexed relationship projection to keep API queries parameterized and
predictable; arbitrary edge properties remain JSONB. Proximity searches use the
latest accepted coordinates and return vehicles ordered by great-circle
distance.
