# Device Registry

Owns vehicle identity, metadata, lifecycle status, and the durable device projection
events consumed by Fleet Topology.

- Inbound boundary: authenticated Device Registry gRPC API.
- Persistence: PostgreSQL through Hibernate Reactive Panache and Flyway.
- Events: transactional outbox to MQTT (`device-projection.v1`).
- Consistency: a device change and its outbox record commit atomically; publication is
  at least once.
- Verify: `mvn -pl services/device-registry -am verify`.

Device keys and repository queries are tenant-scoped, and projection events carry the
originating tenant. Production multi-tenancy still requires downstream projections
and MQTT device identity to enforce the same boundary. See
[`../../docs/architecture-baseline.md`](../../docs/architecture-baseline.md).
