#!/bin/bash
# Script to start backend with external PostgreSQL (Docker) + Backend in Minikube
# This approach matches production architecture: RDS + EKS

echo "🚀 Starting Real Estate Backend (External PostgreSQL + Minikube)"
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
    minikube start --memory=3500 --cpus=2
    echo ""
else
    echo -e "${GREEN}✅ Minikube is running${NC}"
    echo ""
fi

# Start PostgreSQL in Docker
echo "🐘 Starting PostgreSQL in Docker..."
if docker ps | grep -q "real-estate-postgres-local"; then
    echo -e "${GREEN}✅ PostgreSQL is already running${NC}"
else
    docker-compose -f docker-compose-db.yml up -d
    echo "⏳ Waiting for PostgreSQL to be ready..."
    sleep 5

    # Verify PostgreSQL is running
    if docker ps | grep -q "real-estate-postgres-local"; then
        echo -e "${GREEN}✅ PostgreSQL started successfully${NC}"
    else
        echo -e "${RED}❌ Failed to start PostgreSQL${NC}"
        echo "   Check: docker ps"
        echo "   Logs:  docker logs real-estate-postgres-local"
        exit 1
    fi
fi
echo ""

# Check if we need to build the backend image
echo "🔨 Building Backend Docker Image..."
# Point Docker to Minikube's daemon
eval $(minikube docker-env)

# Check if image exists in Minikube
if docker images | grep -q "real-estate-backend.*latest"; then
    echo -e "${YELLOW}ℹ️  Backend image already exists in Minikube${NC}"
    read -p "   Rebuild image? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "   Building new image..."
        mvn clean package -DskipTests
        docker build -t real-estate-backend:latest .
        echo -e "${GREEN}✅ Image rebuilt${NC}"
    fi
else
    echo "   Building backend image..."
    mvn clean package -DskipTests
    docker build -t real-estate-backend:latest .
    echo -e "${GREEN}✅ Image built successfully${NC}"
fi
echo ""

# Deploy to Kubernetes
echo "☸️  Deploying Backend to Kubernetes..."

# Create namespace if it doesn't exist
kubectl create namespace real-estate 2>/dev/null || true

if helm list -n real-estate 2>/dev/null | grep -q "real-estate-backend"; then
    echo -e "${YELLOW}ℹ️  Helm release already exists. Upgrading...${NC}"
    helm upgrade real-estate-backend ./helm/real-estate-backend \
      -f ./helm/real-estate-backend/values-local.yaml \
      -n real-estate
else
    echo "   Installing Helm release..."
    helm install real-estate-backend ./helm/real-estate-backend \
      -f ./helm/real-estate-backend/values-local.yaml \
      -n real-estate
fi
echo ""

# Wait for pods to be ready
echo "⏳ Waiting for pods to be ready (this may take 1-2 minutes)..."
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/component=backend \
  -n real-estate --timeout=120s 2>/dev/null

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Backend pod is ready${NC}"
else
    echo -e "${YELLOW}⚠️  Pod is taking longer than expected...${NC}"
    echo "   Current status:"
    kubectl get pods -n real-estate
fi
echo ""

# Display pod status
echo "📊 Pod Status:"
kubectl get pods -n real-estate
echo ""

# Test the connection via port-forward (works on all platforms)
echo "🔍 Testing connection..."

# Kill any existing port-forward on 8080
pkill -f "port-forward.*8080:8080" 2>/dev/null
sleep 1

# Start port-forward in background
kubectl port-forward svc/real-estate-backend-backend 8080:8080 -n real-estate > /dev/null 2>&1 &
PF_PID=$!
sleep 3  # Give it a moment to establish connection

BACKEND_URL="http://localhost:8080"

if curl -s ${BACKEND_URL}/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Backend is accessible${NC}"
    echo ""
    echo "🎉 Backend services are ready!"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📍 Access URLs (via port-forward):"
    echo "   Backend:       ${BACKEND_URL}"
    echo "   Health Check:  ${BACKEND_URL}/actuator/health"
    echo "   API Docs:      ${BACKEND_URL}/swagger-ui.html"
    echo ""
    echo "🔌 Port-forward is running (PID: ${PF_PID})"
    echo "   To stop: kill ${PF_PID}"
    echo "   To restart: kubectl port-forward svc/real-estate-backend-backend 8080:8080 -n real-estate &"
    echo ""
    echo "🐘 PostgreSQL Connection (Docker):"
    echo "   Host:     localhost"
    echo "   Port:     5432"
    echo "   Database: realestatedb"
    echo "   User:     postgres"
    echo "   Password: postgres"
    echo "   Connect:  psql -h localhost -p 5432 -U postgres -d realestatedb"
    echo ""
    echo "📦 Useful Commands:"
    echo "   Backend logs:  kubectl logs -f deployment/real-estate-backend-backend -n real-estate"
    echo "   DB logs:       docker logs -f real-estate-postgres-local"
    echo "   View pods:     kubectl get pods -n real-estate"
    echo "   Status:        ./k8s-status-external.sh"
    echo "   Stop all:      ./k8s-stop-external.sh"
    echo ""
    echo "🔧 Rebuild and Redeploy:"
    echo "   eval \$(minikube docker-env)"
    echo "   mvn clean package -DskipTests"
    echo "   docker build -t real-estate-backend:latest ."
    echo "   kubectl rollout restart deployment/real-estate-backend-backend -n real-estate"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
else
    # Kill the failed port-forward
    kill $PF_PID 2>/dev/null

    echo -e "${RED}❌ Failed to connect to backend${NC}"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "   1. Check pod logs:   kubectl logs -n real-estate -l app.kubernetes.io/component=backend"
    echo "   2. Check pod status: kubectl describe pod -n real-estate -l app.kubernetes.io/component=backend"
    echo "   3. Check events:     kubectl get events -n real-estate --sort-by='.lastTimestamp'"
    echo "   4. Check PostgreSQL: docker logs real-estate-postgres-local"
    echo "   5. Test DB from pod: kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- sh"
    echo "                        Then: ping host.docker.internal"
    echo "                              nc -zv host.docker.internal 5432"
    echo ""
    echo "   6. Manual port-forward:"
    echo "      kubectl port-forward svc/real-estate-backend-backend 8080:8080 -n real-estate &"
    echo "      curl http://localhost:8080/actuator/health"
fi
echo ""
