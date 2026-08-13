# Vehicle Simulator

The simulator produces representative vehicle telemetry for local demonstrations
and end-to-end testing. It publishes tenant-qualified MQTT messages using the same topic and payload
contract consumed by `telemetry-ingestion`.

## Boundaries

- Generates vehicle position and telemetry values.
- Manages the MQTT client connection and publication lifecycle.
- Does not bypass service APIs or write directly to FleetIQ databases.

The simulator is a test and demonstration client, not a source of production domain
logic. Contract changes must remain aligned with the ingestion adapter.

Set `FLEETIQ_TENANT_ID` to choose the simulated tenant; it defaults to `demo`.

## Verify

```shell
mvn -pl simulator -am test
```
