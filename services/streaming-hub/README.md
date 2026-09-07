# Streaming Hub

Turns the real MQTT telemetry stream into filtered, throttled gRPC server streams for
fleet and individual-vehicle subscribers.

- Inbound boundary: authenticated server-streaming gRPC API.
- Event source: broadcast MQTT telemetry channel.
- State: stateless; subscriptions are represented by reactive streams rather than an
  in-memory subscriber registry.
- Backpressure: each stream has a configurable 256-event default buffer. A client
  that cannot keep up beyond that bound is failed instead of growing memory without limit.
- Admission control: each authenticated tenant/subject has a configurable concurrent
  stream limit, and fleet selections are capped at 500 VINs by default. Cancellation,
  completion, and failure all release the admission slot.
- Tenant isolation: authenticated gRPC streams filter tenant-qualified MQTT events by
  tenant before applying optional VIN selection.
- Verify: `mvn -pl services/streaming-hub -am verify`.

Production MQTT ACLs must bind publishers to their tenant-qualified topic prefix.
Streams are live-only for the MVB; clients reconnect explicitly and do not receive replay.
