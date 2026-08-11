#!/bin/bash
# FleetIQ Development Environment Stopper

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_DIR="$PROJECT_ROOT/infra/docker-compose"

echo "Stopping FleetIQ development environment..."

cd "$COMPOSE_DIR"
docker compose down

echo ""
echo "Infrastructure stopped."
echo "Data volumes preserved. To remove all data:"
echo "  docker compose down -v"
