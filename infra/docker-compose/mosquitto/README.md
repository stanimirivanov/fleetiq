# MQTT identities and topic authorization

Mosquitto rejects anonymous clients. Its startup script creates local-development
passwords and a least-privilege ACL from environment variables.

Backend identities have narrowly scoped permissions:

- `telemetry-ingestion` reads tenant telemetry and publishes position projections.
- `device-registry` publishes device projections.
- `fleet-topology` reads device and position projections.
- `streaming-hub` reads tenant telemetry.
- The simulator username is its tenant id (for example, `demo`) and may publish
  only to `fleetiq/<username>/<vin>/telemetry`.

The defaults in `docker-compose.yml` are intentionally development-only. Override
them with `*_MQTT_PASSWORD` environment variables outside a developer machine.
`FLEETIQ_TENANT_ID` and `FLEETIQ_MQTT_USERNAME` must match for the simulator.

The Kubernetes base contains placeholder Secret values so it can render on its
own. A deployment pipeline must replace those values using a secret manager or
an encrypted overlay before applying the manifests.

The multi-vehicle simulator uses one tenant credential. Real devices should use
one credential (or client certificate) per device, with the broker deriving both
tenant and VIN authorization from that authenticated identity.
