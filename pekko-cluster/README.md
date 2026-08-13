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
- Does not own durable telemetry history or the device registry.

The module is deliberately isolated from the Quarkus services. New integrations
should depend on `VehicleStateService`, not actor implementation classes.

## Verify

```shell
mvn -pl pekko-cluster -am test
```

Cluster deployment, persistence, and recovery policy are not yet production-ready.
Any future persistence ID must retain the same tenant-and-VIN identity.
