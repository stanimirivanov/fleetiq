#!/bin/sh
set -eu

runtime_dir=/mosquitto/runtime
password_file="$runtime_dir/passwords"
acl_file="$runtime_dir/acl"

: "${TELEMETRY_INGESTION_PASSWORD:?TELEMETRY_INGESTION_PASSWORD is required}"
: "${DEVICE_REGISTRY_PASSWORD:?DEVICE_REGISTRY_PASSWORD is required}"
: "${FLEET_TOPOLOGY_PASSWORD:?FLEET_TOPOLOGY_PASSWORD is required}"
: "${STREAMING_HUB_PASSWORD:?STREAMING_HUB_PASSWORD is required}"
: "${SIMULATOR_TENANT:?SIMULATOR_TENANT is required}"
: "${SIMULATOR_PASSWORD:?SIMULATOR_PASSWORD is required}"
: "${REFERENCE_DEVICE_VIN:?REFERENCE_DEVICE_VIN is required}"
: "${REFERENCE_DEVICE_PASSWORD:?REFERENCE_DEVICE_PASSWORD is required}"

mkdir -p "$runtime_dir"
mosquitto_passwd -b -c "$password_file" telemetry-ingestion "$TELEMETRY_INGESTION_PASSWORD"
mosquitto_passwd -b "$password_file" device-registry "$DEVICE_REGISTRY_PASSWORD"
mosquitto_passwd -b "$password_file" fleet-topology "$FLEET_TOPOLOGY_PASSWORD"
mosquitto_passwd -b "$password_file" streaming-hub "$STREAMING_HUB_PASSWORD"
mosquitto_passwd -b "$password_file" "$SIMULATOR_TENANT" "$SIMULATOR_PASSWORD"
mosquitto_passwd -b "$password_file" \
    "$SIMULATOR_TENANT/$REFERENCE_DEVICE_VIN" "$REFERENCE_DEVICE_PASSWORD"
chmod 600 "$password_file"

cat > "$acl_file" <<'EOF'
user telemetry-ingestion
topic read fleetiq/+/+/telemetry
topic write fleetiq/events/position-projections

user device-registry
topic write fleetiq/events/device-projections

user fleet-topology
topic read fleetiq/events/device-projections
topic read fleetiq/events/position-projections

user streaming-hub
topic read fleetiq/+/+/telemetry

# Development-only multi-vehicle simulator identity.
pattern write fleetiq/%u/+/telemetry

# Device identities use <tenant>/<vin>; the username expands to the exact topic prefix.
pattern write fleetiq/%u/telemetry
EOF
chmod 600 "$acl_file"

exec mosquitto -c /mosquitto/config/mosquitto.conf
