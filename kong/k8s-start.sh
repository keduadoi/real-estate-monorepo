#!/bin/bash
# Script to start Kong API Gateway in Kubernetes

echo "🚀 Starting Kong API Gateway"
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if minikube is running
echo "🖥️  Checking Minikube..."
if ! minikube status > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Minikube is not running. Starting minikube...${NC}"
    minikube start --memory=4096 --cpus=2
    echo ""
else
    echo -e "${GREEN}✅ Minikube is running${NC}"
    echo ""
fi

# Deploy to Kubernetes
echo "☸️  Deploying Kong Gateway to Kubernetes..."

# Create namespace if it doesn't exist
kubectl create namespace kong 2>/dev/null || true

if helm list -n kong 2>/dev/null | grep -q "kong-gateway"; then
    echo -e "${YELLOW}ℹ️  Helm release already exists. Upgrading...${NC}"
    helm upgrade kong-gateway ./helm/kong \
      -f ./helm/kong/values-local.yaml \
      -n kong
else
    echo "   Installing Helm release..."
    helm install kong-gateway ./helm/kong \
      -f ./helm/kong/values-local.yaml \
      -n kong
fi
echo ""

# Wait for pods to be ready
echo "⏳ Waiting for Kong pods to be ready (this may take 2-3 minutes)..."

# Wait for PostgreSQL first
echo "   Waiting for Kong PostgreSQL..."
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/name=kong-postgres \
  -n kong --timeout=120s 2>/dev/null || true

# Wait for Kong migrations
echo "   Waiting for Kong migrations..."
kubectl wait --for=condition=complete job \
  -l app.kubernetes.io/name=kong-migrations \
  -n kong --timeout=180s 2>/dev/null || true

# Wait for Kong
echo "   Waiting for Kong..."
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/name=kong \
  -n kong --timeout=120s 2>/dev/null

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Kong pod is ready${NC}"
else
    echo -e "${YELLOW}⚠️  Pod is taking longer than expected...${NC}"
    echo "   Current status:"
    kubectl get pods -n kong
fi
echo ""

# Display pod status
echo "📊 Pod Status:"
kubectl get pods -n kong
echo ""

# Test the connection via port-forward
echo "🔍 Testing connection..."

# Kill any existing port-forward
pkill -f "port-forward.*8000:80" 2>/dev/null
pkill -f "port-forward.*8001:8001" 2>/dev/null
sleep 1

# Start port-forward in background
kubectl port-forward svc/kong-proxy 8000:80 -n kong > /dev/null 2>&1 &
PF_PROXY_PID=$!

kubectl port-forward svc/kong-admin 8001:8001 -n kong > /dev/null 2>&1 &
PF_ADMIN_PID=$!

sleep 3

if curl -s http://localhost:8001/status > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Kong Gateway is accessible${NC}"
    echo ""
    echo "🎉 Kong Gateway is ready!"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📍 Access URLs (via port-forward):"
    echo "   Kong Proxy:    http://localhost:8000"
    echo "   Kong Admin:    http://localhost:8001"
    echo "   Kong Status:   http://localhost:8001/status"
    echo ""
    echo "🔌 Port-forwards are running:"
    echo "   Proxy PID: ${PF_PROXY_PID}"
    echo "   Admin PID: ${PF_ADMIN_PID}"
    echo "   To stop: pkill -f 'port-forward.*kong'"
    echo ""
    echo "📦 Useful Commands:"
    echo "   Kong logs:     kubectl logs -f deployment/kong -n kong"
    echo "   List routes:   curl http://localhost:8001/routes"
    echo "   List services: curl http://localhost:8001/services"
    echo "   View pods:     kubectl get pods -n kong"
    echo "   Status:        ./k8s-status.sh"
    echo "   Stop:          ./k8s-stop.sh"
    echo ""
    echo "🔧 Configure Routes:"
    echo "   ./scripts/setup-kong-routes.sh"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
else
    kill $PF_PROXY_PID $PF_ADMIN_PID 2>/dev/null

    echo -e "${RED}❌ Failed to connect to Kong${NC}"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "   1. Check pod logs:   kubectl logs -n kong -l app.kubernetes.io/name=kong"
    echo "   2. Check pod status: kubectl describe pod -n kong -l app.kubernetes.io/name=kong"
    echo "   3. Check events:     kubectl get events -n kong --sort-by='.lastTimestamp'"
    echo "   4. Check migrations: kubectl logs -n kong -l app.kubernetes.io/name=kong-migrations"
    echo ""
    echo "   5. Manual port-forward:"
    echo "      kubectl port-forward svc/kong-proxy 8000:80 -n kong &"
    echo "      kubectl port-forward svc/kong-admin 8001:8001 -n kong &"
fi
echo ""
