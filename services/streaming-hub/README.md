# Streaming Hub

Turns the real MQTT telemetry stream into filtered, throttled gRPC server streams for
fleet and individual-vehicle subscribers.

- Inbound boundary: authenticated server-streaming gRPC API.
- Event source: broadcast MQTT telemetry channel.
- State: stateless; subscriptions are represented by reactive streams rather than an
  in-memory subscriber registry.
- Backpressure: Mutiny stream composition preserves cancellation and failure signals.
- Tenant isolation: authenticated gRPC streams filter tenant-qualified MQTT events by
  tenant before applying optional VIN selection.
- Verify: `mvn -pl services/streaming-hub -am verify`.

Production MQTT ACLs must bind publishers to their tenant-qualified topic prefix.
