#!/bin/bash
# Script to start post-service with external PostgreSQL (Docker) + Service in Minikube

echo "🚀 Starting Post Service (External PostgreSQL + Minikube)"
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

# Start PostgreSQL in Docker
echo "🐘 Starting PostgreSQL in Docker..."
if docker ps | grep -q "post-db"; then
    echo -e "${GREEN}✅ PostgreSQL (post-db) is already running${NC}"
else
    docker-compose -f docker-compose-db.yml up -d
    echo "⏳ Waiting for PostgreSQL to be ready..."
    sleep 5

    # Verify PostgreSQL is running
    if docker ps | grep -q "post-db"; then
        echo -e "${GREEN}✅ PostgreSQL started successfully${NC}"
    else
        echo -e "${RED}❌ Failed to start PostgreSQL${NC}"
        echo "   Check: docker ps"
        echo "   Logs:  docker logs post-db"
        exit 1
    fi
fi
echo ""

# Check if we need to build the service image
echo "🔨 Building Post Service Docker Image..."
# Point Docker to Minikube's daemon
eval $(minikube docker-env)

# Check if image exists in Minikube
if docker images | grep -q "post-service.*latest"; then
    echo -e "${YELLOW}ℹ️  Post service image already exists in Minikube${NC}"
    read -p "   Rebuild image? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "   Building new image..."
        mvn clean package -DskipTests
        docker build -t post-service:latest .
        echo -e "${GREEN}✅ Image rebuilt${NC}"
    fi
else
    echo "   Building post-service image..."
    mvn clean package -DskipTests
    docker build -t post-service:latest .
    echo -e "${GREEN}✅ Image built successfully${NC}"
fi
echo ""

# Deploy to Kubernetes
echo "☸️  Deploying Post Service to Kubernetes..."

# Create namespace if it doesn't exist
kubectl create namespace real-estate 2>/dev/null || true

if helm list -n real-estate 2>/dev/null | grep -q "post-service"; then
    echo -e "${YELLOW}ℹ️  Helm release already exists. Upgrading...${NC}"
    helm upgrade post-service ./helm/post-service \
      -f ./helm/post-service/values-local.yaml \
      -n real-estate
else
    echo "   Installing Helm release..."
    helm install post-service ./helm/post-service \
      -f ./helm/post-service/values-local.yaml \
      -n real-estate
fi
echo ""

# Wait for pods to be ready
echo "⏳ Waiting for pods to be ready (this may take 1-2 minutes)..."
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/name=post-service \
  -n real-estate --timeout=120s 2>/dev/null

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Post service pod is ready${NC}"
else
    echo -e "${YELLOW}⚠️  Pod is taking longer than expected...${NC}"
    echo "   Current status:"
    kubectl get pods -n real-estate -l app.kubernetes.io/name=post-service
fi
echo ""

# Display pod status
echo "📊 Pod Status:"
kubectl get pods -n real-estate -l app.kubernetes.io/name=post-service
echo ""

# Test the connection via port-forward
echo "🔍 Testing connection..."

# Kill any existing port-forward on 8082
pkill -f "port-forward.*8082:8082" 2>/dev/null
sleep 1

# Start port-forward in background
kubectl port-forward svc/post-service 8082:8082 -n real-estate > /dev/null 2>&1 &
PF_PID=$!
sleep 3

SERVICE_URL="http://localhost:8082"

if curl -s ${SERVICE_URL}/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Post service is accessible${NC}"
    echo ""
    echo "🎉 Post service is ready!"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📍 Access URLs (via port-forward):"
    echo "   Post Service:  ${SERVICE_URL}"
    echo "   Health Check:  ${SERVICE_URL}/actuator/health"
    echo "   API Docs:      ${SERVICE_URL}/swagger-ui.html"
    echo ""
    echo "🔌 Port-forward is running (PID: ${PF_PID})"
    echo "   To stop: kill ${PF_PID}"
    echo "   To restart: kubectl port-forward svc/post-service 8082:8082 -n real-estate &"
    echo ""
    echo "🐘 PostgreSQL Connection (Docker):"
    echo "   Host:     localhost"
    echo "   Port:     5434"
    echo "   Database: postdb"
    echo "   User:     postgres"
    echo "   Password: postgres"
    echo "   Connect:  psql -h localhost -p 5434 -U postgres -d postdb"
    echo ""
    echo "📦 Useful Commands:"
    echo "   Service logs:  kubectl logs -f deployment/post-service -n real-estate"
    echo "   DB logs:       docker logs -f post-db"
    echo "   View pods:     kubectl get pods -n real-estate"
    echo "   Status:        ./k8s-status-external.sh"
    echo "   Stop all:      ./k8s-stop-external.sh"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
else
    kill $PF_PID 2>/dev/null

    echo -e "${RED}❌ Failed to connect to post service${NC}"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "   1. Check pod logs:   kubectl logs -n real-estate -l app.kubernetes.io/name=post-service"
    echo "   2. Check pod status: kubectl describe pod -n real-estate -l app.kubernetes.io/name=post-service"
    echo "   3. Check events:     kubectl get events -n real-estate --sort-by='.lastTimestamp'"
    echo "   4. Check PostgreSQL: docker logs post-db"
    echo ""
    echo "   5. Manual port-forward:"
    echo "      kubectl port-forward svc/post-service 8082:8082 -n real-estate &"
    echo "      curl http://localhost:8082/actuator/health"
fi
echo ""
