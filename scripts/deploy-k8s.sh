#!/bin/bash
set -e
cd "$(dirname "$0")/.."
echo "Deploying FleetIQ to Kubernetes..."
kubectl apply -k infra/kubernetes/overlays/dev
echo "Deployment triggered."
