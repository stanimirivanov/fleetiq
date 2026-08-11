.PHONY: help dev-start dev-stop build test clean deploy-k8s

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ── Development Environment ──────────────────────────────────

dev-start: ## Start infrastructure services (Docker Compose)
	@./scripts/start-dev.sh

dev-stop: ## Stop infrastructure services
	@./scripts/stop-dev.sh

dev-telemetry: ## Start telemetry-ingestion in dev mode
	@cd services/telemetry-ingestion && mvn quarkus:dev

dev-device: ## Start device-registry in dev mode
	@cd services/device-registry && mvn quarkus:dev

dev-topology: ## Start fleet-topology in dev mode
	@cd services/fleet-topology && mvn quarkus:dev

dev-maintenance: ## Start maintenance-predictor in dev mode
	@cd services/maintenance-predictor && mvn quarkus:dev

dev-streaming: ## Start streaming-hub in dev mode
	@cd services/streaming-hub && mvn quarkus:dev

dev-pekko: ## Start Pekko cluster locally
	@cd pekko-cluster && mvn compile exec:java

# ── Build ────────────────────────────────────────────────────

build: ## Build all modules
	@./scripts/build-all.sh

build-native: ## Build all services as GraalVM native images
	@mvn clean package -Pnative -DskipTests

test: ## Run all tests
	@mvn clean verify

test-unit: ## Run unit tests only
	@mvn test -Dgroups="unit"

test-integration: ## Run integration tests
	@mvn test -Dgroups="integration"

# ── Docker Images ────────────────────────────────────────────

docker-build: ## Build Docker images for all services
	@mvn clean package -Dquarkus.container-image.build=true

docker-push: ## Build and push Docker images
	@mvn clean package -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true

# ── Kubernetes ───────────────────────────────────────────────

deploy-k8s: ## Deploy to Kubernetes using Kustomize
	@./scripts/deploy-k8s.sh

deploy-k8s-dev: ## Deploy dev overlay
	@kubectl apply -k infra/kubernetes/overlays/dev

# ── Cleanup ──────────────────────────────────────────────────

clean: ## Clean all build artifacts
	@mvn clean
	@rm -rf .quarkus/

# ── Utilities ────────────────────────────────────────────────

logs: ## Tail logs from Docker Compose services
	@cd infra/docker-compose && docker compose logs -f

psql-telemetry: ## Connect to telemetry database
	@docker exec -it fleetiq-postgres psql -U fleetiq -d telemetry_db

psql-device: ## Connect to device registry database
	@docker exec -it fleetiq-postgres psql -U fleetiq -d device_registry_db

psql-topology: ## Connect to topology database
	@docker exec -it fleetiq-postgres psql -U fleetiq -d topology_db

psql-maintenance: ## Connect to maintenance database
	@docker exec -it fleetiq-postgres psql -U fleetiq -d maintenance_db

mqtt-sub: ## Subscribe to all MQTT topics
	@docker exec -it fleetiq-mosquitto mosquitto_sub -t "fleetiq/#" -v
