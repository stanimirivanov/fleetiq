#!/bin/bash
# Vault Development Initialization
# Configures Vault dev instance with FleetIQ secrets

set -e

VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="fleetiq-dev-token"

export VAULT_ADDR VAULT_TOKEN

echo "==> Waiting for Vault to be ready..."
until vault status > /dev/null 2>&1; do
    sleep 1
done

echo "==> Configuring Vault for FleetIQ development..."

# Enable database secrets engine
vault secrets enable database || true

# Configure PostgreSQL connection
vault write database/config/fleetiq-postgres \
    plugin_name=postgresql-database-plugin \
    allowed_roles="fleetiq-readonly,fleetiq-readwrite" \
    connection_url="postgresql://{{username}}:{{password}}@postgres:5432/telemetry_db?sslmode=disable" \
    username="fleetiq" \
    password="fleetiq_dev"

# Create roles for dynamic credentials
vault write database/roles/fleetiq-readonly \
    db_name=fleetiq-postgres \
    creation_statements="CREATE USER \"{{name}}\" WITH PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

vault write database/roles/fleetiq-readwrite \
    db_name=fleetiq-postgres \
    creation_statements="CREATE USER \"{{name}}\" WITH PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="4h"

# Enable KV v2 secrets engine for general secrets
vault secrets enable -path=fleetiq kv-v2 || true

# Store development secrets
vault kv put fleetiq/mqtt username="fleetiq-device" password="mqtt-dev-password"
vault kv put fleetiq/pekko/cluster secret="pekko-cluster-dev-secret"

# Enable transit engine for encryption-as-a-service
vault secrets enable transit || true

echo "==> Vault initialization complete!"
echo ""
echo "    Address:  $VAULT_ADDR"
echo "    Token:    $VAULT_TOKEN"
