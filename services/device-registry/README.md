# Device Registry

Owns vehicle identity, metadata, lifecycle status, and the durable device projection
events consumed by Fleet Topology.

- Inbound boundary: authenticated Device Registry gRPC API.
- Persistence: PostgreSQL through Hibernate Reactive Panache and Flyway.
- Events: transactional outbox to MQTT (`device-projection.v1`).
- Consistency: a device change and its outbox record commit atomically; publication is
  at least once.
- Verify: `mvn -pl services/device-registry -am verify`.

Tenant identity is established at the API boundary, but tenant-scoped keys and rows
are still an explicit prerequisite for production multi-tenancy. See
[`../../docs/architecture-baseline.md`](../../docs/architecture-baseline.md).
