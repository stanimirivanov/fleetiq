#!/bin/bash
set -e
cd "$(dirname "$0")/.."
echo "Building all FleetIQ modules..."
mvn clean compile -DskipTests
echo "Build complete."
