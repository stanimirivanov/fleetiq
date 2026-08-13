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
- A production-shaped device username is `<tenant>/<vin>` and may publish only
  to `fleetiq/<tenant>/<vin>/telemetry`. The local reference identity is
  `demo/SIM-VIN-001`; its password comes from `REFERENCE_DEVICE_MQTT_PASSWORD`.

The defaults in `docker-compose.yml` are intentionally development-only. Override
them with `*_MQTT_PASSWORD` environment variables outside a developer machine.
`FLEETIQ_TENANT_ID` and `FLEETIQ_MQTT_USERNAME` must match for the simulator.

The Kubernetes base contains placeholder Secret values so it can render on its
own. A deployment pipeline must replace those values using a secret manager or
an encrypted overlay before applying the manifests.

The multi-vehicle simulator tenant credential is a development-only exception.
Real devices use one credential (or client certificate) per device. FleetIQ
encodes the authenticated principal as `<tenant>/<vin>`, allowing Mosquitto to
derive both parts of the authorized topic without trusting message payload data.
Device Registry will eventually provision and revoke these identities through a
broker credential-management adapter; credentials must never be returned by its
ordinary device query API.
