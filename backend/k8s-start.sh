#!/bin/bash
# Script to start Kubernetes backend and set up port forwarding

echo "🚀 Starting Kubernetes Backend..."
echo ""

# Check if minikube is running
if ! minikube status > /dev/null 2>&1; then
    echo "⚠️  Minikube is not running. Starting minikube..."
    minikube start --memory=3500 --cpus=2
    echo ""
fi

# Check if helm release is installed
if ! helm list | grep -q "real-estate"; then
    echo "⚠️  Helm release not found. Installing..."
    helm install real-estate ./helm/real-estate-backend \
      -f ./helm/real-estate-backend/values-dev.yaml
    echo ""
    echo "⏳ Waiting for pods to be ready (this may take 1-2 minutes)..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/component=backend -n real-estate-dev --timeout=120s
    echo ""
fi

# Check pod status
echo "📊 Checking pod status..."
kubectl get pods -n real-estate-dev
echo ""

# Kill any existing port-forward on port 8080 and 5432
echo "🔌 Setting up port forwarding..."
lsof -ti:8080 | xargs kill -9 2>/dev/null
lsof -ti:5432 | xargs kill -9 2>/dev/null

# Start backend port-forward in background
kubectl port-forward -n real-estate-dev \
  svc/real-estate-real-estate-backend-backend 8080:8080 \
  > /tmp/k8s-port-forward.log 2>&1 &
BACKEND_PF_PID=$!

# Start PostgreSQL port-forward in background
kubectl port-forward -n real-estate-dev \
  svc/real-estate-real-estate-backend-postgres 5432:5432 \
  > /tmp/k8s-postgres-port-forward.log 2>&1 &
POSTGRES_PF_PID=$!

echo "✅ Port forwards started:"
echo "   Backend:    localhost:8080 (PID: $BACKEND_PF_PID) - Logs: /tmp/k8s-port-forward.log"
echo "   PostgreSQL: localhost:5432 (PID: $POSTGRES_PF_PID) - Logs: /tmp/k8s-postgres-port-forward.log"
echo ""

# Wait for port-forward to be ready
sleep 3

# Test the connection
echo "🔍 Testing connection..."
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ Backend is accessible at http://localhost:8080"
    echo ""
    echo "🎉 Kubernetes backend is ready!"
    echo ""
    echo "📍 Useful commands:"
    echo "   Health check:  curl http://localhost:8080/actuator/health"
    echo "   View logs:     kubectl logs -n real-estate-dev -l app.kubernetes.io/component=backend -f"
    echo "   View pods:     kubectl get pods -n real-estate-dev"
    echo "   Stop:          ./k8s-stop.sh"
    echo ""
    echo "📦 PostgreSQL connection:"
    echo "   Host: localhost | Port: 5432 | DB: realestatedb | User: postgres | Pass: postgres"
else
    echo "❌ Failed to connect to backend"
    echo "   Check logs: tail -f /tmp/k8s-port-forward.log"
    echo "   Check pods: kubectl get pods -n real-estate-dev"
fi
