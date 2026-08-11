#!/bin/bash
# FleetIQ Development Environment Starter
# Starts infrastructure services and provides instructions for app services

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_DIR="$PROJECT_ROOT/infra/docker-compose"

echo "============================================"
echo "  FleetIQ — Development Environment"
echo "============================================"
echo ""

# Check prerequisites
command -v docker >/dev/null 2>&1 || {
    echo "ERROR: Docker is required but not installed."
    exit 1
}

command -v java >/dev/null 2>&1 || {
    echo "ERROR: Java 21+ is required but not installed."
    exit 1
}

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "WARNING: Java 21+ recommended. Found version: $JAVA_VERSION"
fi

echo "[1/3] Starting infrastructure services..."
cd "$COMPOSE_DIR"
docker compose up -d

echo ""
echo "[2/3] Waiting for services to be healthy..."

# Wait for PostgreSQL
echo "  Waiting for PostgreSQL..."
until docker compose exec -T postgres pg_isready -U fleetiq > /dev/null 2>&1; do
    sleep 2
done
echo "  ✓ PostgreSQL ready"

# Wait for Mosquitto
echo "  Waiting for Mosquitto..."
until docker compose exec -T mosquitto mosquitto_sub -t '$SYS/broker/version' -C 1 > /dev/null 2>&1; do
    sleep 2
done
echo "  ✓ Mosquitto ready"

# Wait for Keycloak
echo "  Waiting for Keycloak..."
until curl -s http://localhost:8080/health > /dev/null 2>&1; do
    sleep 3
done
echo "  ✓ Keycloak ready"

# Wait for Vault
echo "  Waiting for Vault..."
until curl -s http://localhost:8200/v1/sys/health > /dev/null 2>&1; do
    sleep 2
done
echo "  ✓ Vault ready"

echo ""
echo "[3/3] Infrastructure ready!"
echo ""
echo "============================================"
echo "  Services Available"
echo "============================================"
echo ""
echo "  PostgreSQL:    localhost:5432"
echo "    User: fleetiq / fleetiq_dev"
echo "    Databases: telemetry_db, device_registry_db,"
echo "               topology_db, maintenance_db, pekko_journal_db"
echo ""
echo "  Mosquitto:     localhost:1883 (MQTT), localhost:9001 (WS)"
echo ""
echo "  Keycloak:      http://localhost:8080"
echo "    Admin: admin / admin"
echo ""
echo "  Vault:         http://localhost:8200"
echo "    Token: fleetiq-dev-token"
echo ""
echo "  Grafana:       http://localhost:3000"
echo "  Prometheus:    http://localhost:9090"
echo ""
echo "============================================"
echo "  Starting Application Services"
echo "============================================"
echo ""
echo "Run each service in a separate terminal using Quarkus dev mode:"
echo ""
echo "  cd services/telemetry-ingestion"
echo "  mvn quarkus:dev"
echo ""
echo "  cd services/device-registry"
echo "  mvn quarkus:dev"
echo ""
echo "  cd services/fleet-topology"
echo "  mvn quarkus:dev"
echo ""
echo "  cd services/maintenance-predictor"
echo "  mvn quarkus:dev"
echo ""
echo "  cd services/streaming-hub"
echo "  mvn quarkus:dev"
echo ""
echo "  cd pekko-cluster"
echo "  mvn compile exec:java"
echo ""
echo "Or use tmux/screen to run all at once."
echo ""
echo "To stop: $PROJECT_ROOT/scripts/stop-dev.sh"
