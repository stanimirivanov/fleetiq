# Telemetry Ingestion

Accepts vehicle telemetry over MQTT and gRPC, persists time-series samples, and emits
durable position projection events for Fleet Topology.

- Inbound boundaries: tenant-qualified MQTT topic
  (`fleetiq/{tenant}/{vin}/telemetry`) and authenticated gRPC API.
- Persistence: TimescaleDB hypertables and continuous aggregates managed by Flyway.
- Events: transactional outbox to MQTT (`position-projection.v1`).
- Consistency: telemetry and its position event commit atomically; publication is at
  least once.
- Verify: `mvn -pl services/telemetry-ingestion -am verify`.

Telemetry rows, queries, aggregates, and position events are tenant-scoped. A production
MQTT broker must authenticate publishers and enforce an ACL matching the authenticated
tenant to the topic tenant; topic text alone is not an authentication mechanism.
