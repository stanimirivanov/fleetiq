# Telemetry Ingestion

Accepts vehicle telemetry over MQTT and gRPC, persists time-series samples, and emits
durable position projection events for Fleet Topology.

- Inbound boundaries: MQTT telemetry topic and authenticated gRPC API.
- Persistence: TimescaleDB hypertables and continuous aggregates managed by Flyway.
- Events: transactional outbox to MQTT (`position-projection.v1`).
- Consistency: telemetry and its position event commit atomically; publication is at
  least once.
- Verify: `mvn -pl services/telemetry-ingestion -am verify`.

MQTT device authentication and device-to-tenant mapping are required before enabling
production multi-tenancy.
