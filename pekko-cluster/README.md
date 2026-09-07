# Pekko Cluster

This module is FleetIQ's stateful actor boundary. It owns per-vehicle runtime state
and hides Pekko sharding, serialization, and ask-pattern details behind
`VehicleStateService`.

## Boundaries

- Accepts validated telemetry updates and vehicle commands through a Java API.
- Locates vehicle actors through cluster sharding.
- Returns asynchronous outcomes without exposing actor references to callers.
- Uses a tenant-and-VIN shard identity so identical VINs in different tenants never
  share actor state or sequence numbers.
- Persists accepted telemetry and vehicle commands as events under the same stable
  tenant/VIN identity, with snapshots every 100 events and two snapshots retained.
- Does not own durable telemetry history or the device registry.

The module is deliberately isolated from the Quarkus services. New integrations
should depend on `VehicleStateService`, not actor implementation classes.

## Durable behavior

`VehicleActor` reconstructs its current position, speed, telemetry sequence, and
recent command IDs from two event types: `TelemetryRecorded` and
`VehicleCommandAccepted`. Its rules are intentionally small and explicit:

- newer telemetry advances state;
- an exact telemetry redelivery is accepted without another event;
- older telemetry or conflicting values at the same timestamp are rejected;
- every vehicle command requires a caller-supplied command ID;
- the most recent 1,000 command IDs are retained so redelivery is idempotent.

Production uses Pekko Persistence JDBC with `event_journal` and `snapshot` tables
in `pekko_journal_db`. Local actor tests use the in-memory journal, while
`VehicleActorJdbcIT` starts PostgreSQL and proves state and command-ID recovery by
stopping and recreating an actor with the same persistence ID. The infrastructure
bootstrap scripts create the same production tables.

## Verify

```shell
mvn -pl pekko-cluster -am test
```

Multi-node relocation still requires integration tests.
The tenant-and-VIN persistence identity is a compatibility boundary and must not be
changed without a migration plan.
