#!/bin/bash
# Vault Development Initialization
set -e

VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="fleetiq-dev-token"
export VAULT_ADDR VAULT_TOKEN

echo "==> Waiting for Vault..."
until vault status > /dev/null 2>&1; do sleep 1; done

echo "==> Configuring Vault for FleetIQ..."

# Enable KV v2
vault secrets enable -path=fleetiq kv-v2 2>/dev/null || true

# Store secrets
vault kv put fleetiq/postgres \
    username="fleetiq" \
    password="fleetiq_dev"

vault kv put fleetiq/mqtt \
    username="fleetiq-device" \
    password="mqtt-dev-password"

vault kv put fleetiq/pekko/cluster \
    secret="pekko-cluster-dev-secret"

echo "==> Vault ready."
