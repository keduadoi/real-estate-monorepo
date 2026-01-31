#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KONG_DIR="$SCRIPT_DIR/.."
HELM_DIR="$KONG_DIR/helm/kong"

echo "=== Phase 2: Kong Gateway Setup ==="

# 1. Deploy Kong via Helm
echo "[1/4] Deploying Kong via Helm..."
helm upgrade --install kong-gateway "$HELM_DIR" \
  -f "$HELM_DIR/values-local.yaml" \
  -n kong --create-namespace

# 2. Wait for PostgreSQL
echo "[2/4] Waiting for Kong PostgreSQL..."
kubectl wait --for=condition=ready pod -l app=kong-postgres -n kong --timeout=120s

# 3. Wait for migration job
echo "[3/4] Waiting for Kong migrations..."
sleep 5
kubectl wait --for=condition=complete job/kong-migrations -n kong --timeout=120s 2>/dev/null || {
  echo "Migration job may have already completed or is running. Checking Kong pods..."
}

# 4. Wait for Kong
echo "[4/4] Waiting for Kong gateway..."
kubectl wait --for=condition=ready pod -l app=kong -n kong --timeout=120s

echo ""
echo "=== Kong Gateway deployed ==="
kubectl get pods -n kong
kubectl get svc -n kong
echo ""
echo "Kong proxy:  $(minikube service kong-proxy -n kong --url 2>/dev/null || echo 'Run: minikube service kong-proxy -n kong --url')"
echo "Kong admin:  $(minikube service kong-admin -n kong --url 2>/dev/null || echo 'Run: minikube service kong-admin -n kong --url')"
