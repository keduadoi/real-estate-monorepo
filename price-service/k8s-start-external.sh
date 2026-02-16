#!/bin/bash
# Script to start price-service with external PostgreSQL + Kafka (Docker) + Service in Minikube
# This approach matches production architecture: RDS + MSK + EKS

echo "🚀 Starting Price Service (External PostgreSQL + Kafka + Minikube)"
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
if docker ps | grep -q "price-db"; then
    echo -e "${GREEN}✅ PostgreSQL is already running${NC}"
else
    docker-compose -f docker-compose-db.yml up -d
    echo "⏳ Waiting for PostgreSQL to be ready..."
    sleep 5

    # Verify PostgreSQL is running
    if docker ps | grep -q "price-db"; then
        echo -e "${GREEN}✅ PostgreSQL started successfully${NC}"
    else
        echo -e "${RED}❌ Failed to start PostgreSQL${NC}"
        echo "   Check: docker ps"
        echo "   Logs:  docker logs price-db"
        exit 1
    fi
fi
echo ""

# Start Kafka in Docker (shared instance from kafka/ directory)
echo "📨 Starting Kafka in Docker..."
if docker ps | grep -q "kafka"; then
    echo -e "${GREEN}✅ Kafka is already running${NC}"
else
    # Use the shared Kafka docker-compose from the root kafka/ directory
    docker-compose -f ../kafka/docker-compose.yml up -d
    echo "⏳ Waiting for Kafka to be ready (this may take 30s)..."
    sleep 15

    # Wait for Kafka health check
    RETRIES=10
    while [ $RETRIES -gt 0 ]; do
        if docker exec kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1; then
            break
        fi
        RETRIES=$((RETRIES - 1))
        sleep 5
    done

    if docker ps | grep -q "kafka"; then
        echo -e "${GREEN}✅ Kafka started successfully${NC}"
    else
        echo -e "${RED}❌ Failed to start Kafka${NC}"
        echo "   Check: docker ps"
        echo "   Logs:  docker logs kafka"
        exit 1
    fi
fi
echo ""

# Build grpc-proto dependency first
echo "🔨 Building gRPC Proto dependency..."
cd ../grpc-proto
mvn clean install -DskipTests -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ grpc-proto built and installed${NC}"
else
    echo -e "${RED}❌ Failed to build grpc-proto${NC}"
    exit 1
fi
cd ../price-service
echo ""

# Check if we need to build the price-service image
echo "🔨 Building Price Service Docker Image..."
# Point Docker to Minikube's daemon
eval $(minikube docker-env)

# Check if image exists in Minikube
if docker images | grep -q "price-service.*latest"; then
    echo -e "${YELLOW}ℹ️  Price service image already exists in Minikube${NC}"
    read -p "   Rebuild image? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "   Building new image (includes grpc-proto)..."
        docker build -t price-service:latest -f Dockerfile ..
        echo -e "${GREEN}✅ Image rebuilt${NC}"
    fi
else
    echo "   Building price-service image (includes grpc-proto)..."
    docker build -t price-service:latest -f Dockerfile ..
    echo -e "${GREEN}✅ Image built successfully${NC}"
fi
echo ""

# Deploy to Kubernetes
echo "☸️  Deploying Price Service to Kubernetes..."

# Create namespace if it doesn't exist
kubectl create namespace real-estate 2>/dev/null || true

if helm list -n real-estate 2>/dev/null | grep -q "real-estate-price"; then
    echo -e "${YELLOW}ℹ️  Helm release already exists. Upgrading...${NC}"
    helm upgrade real-estate-price ./helm/real-estate-price \
      -f ./helm/real-estate-price/values-local.yaml \
      -n real-estate
else
    echo "   Installing Helm release..."
    helm install real-estate-price ./helm/real-estate-price \
      -f ./helm/real-estate-price/values-local.yaml \
      -n real-estate
fi
echo ""

# Wait for pods to be ready
echo "⏳ Waiting for pods to be ready (this may take 1-2 minutes)..."
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/component=price \
  -n real-estate --timeout=120s 2>/dev/null

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Price service pod is ready${NC}"
else
    echo -e "${YELLOW}⚠️  Pod is taking longer than expected...${NC}"
    echo "   Current status:"
    kubectl get pods -n real-estate -l app.kubernetes.io/component=price
fi
echo ""

# Display pod status
echo "📊 Pod Status:"
kubectl get pods -n real-estate -l app.kubernetes.io/component=price
echo ""

# Test the connection via port-forward
echo "🔍 Testing connection..."

# Kill any existing port-forwards
pkill -f "port-forward.*8084:8084" 2>/dev/null
pkill -f "port-forward.*9090:9090" 2>/dev/null
sleep 1

# Start port-forwards in background (REST + gRPC)
kubectl port-forward svc/real-estate-price-price 8084:8084 -n real-estate > /dev/null 2>&1 &
PF_REST_PID=$!
kubectl port-forward svc/real-estate-price-price 9090:9090 -n real-estate > /dev/null 2>&1 &
PF_GRPC_PID=$!
sleep 3

PRICE_URL="http://localhost:8084"

if curl -s ${PRICE_URL}/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Price Service is accessible${NC}"
    echo ""
    echo "🎉 Price services are ready!"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📍 Access URLs (via port-forward):"
    echo "   REST API:      ${PRICE_URL}"
    echo "   Health Check:  ${PRICE_URL}/actuator/health"
    echo "   Metrics:       ${PRICE_URL}/actuator/prometheus"
    echo "   gRPC:          localhost:9090"
    echo ""
    echo "🔌 Port-forwards are running:"
    echo "   REST  PID: ${PF_REST_PID} (port 8084)"
    echo "   gRPC  PID: ${PF_GRPC_PID} (port 9090)"
    echo "   To stop: kill ${PF_REST_PID} ${PF_GRPC_PID}"
    echo "   To restart:"
    echo "     kubectl port-forward svc/real-estate-price-price 8084:8084 -n real-estate &"
    echo "     kubectl port-forward svc/real-estate-price-price 9090:9090 -n real-estate &"
    echo ""
    echo "🐘 PostgreSQL Connection (Docker):"
    echo "   Host:     localhost"
    echo "   Port:     5435"
    echo "   Database: pricedb"
    echo "   User:     postgres"
    echo "   Password: postgres"
    echo "   Connect:  psql -h localhost -p 5435 -U postgres -d pricedb"
    echo ""
    echo "📨 Kafka (Docker):"
    echo "   Bootstrap: localhost:29092"
    echo "   Topics:    price-change-events, user-activity-events"
    echo ""
    echo "📦 Useful Commands:"
    echo "   Service logs:  kubectl logs -f deployment/real-estate-price-price -n real-estate"
    echo "   DB logs:       docker logs -f price-db"
    echo "   Kafka logs:    docker logs -f kafka"
    echo "   View pods:     kubectl get pods -n real-estate"
    echo "   Status:        ./k8s-status-external.sh"
    echo "   Stop all:      ./k8s-stop-external.sh"
    echo ""
    echo "🔧 Rebuild and Redeploy:"
    echo "   eval \$(minikube docker-env)"
    echo "   cd ../grpc-proto && mvn clean install -DskipTests && cd ../price-service"
    echo "   docker build -t price-service:latest -f Dockerfile .."
    echo "   kubectl rollout restart deployment/real-estate-price-price -n real-estate"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
else
    # Kill the failed port-forwards
    kill $PF_REST_PID 2>/dev/null
    kill $PF_GRPC_PID 2>/dev/null

    echo -e "${RED}❌ Failed to connect to price service${NC}"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "   1. Check pod logs:   kubectl logs -n real-estate -l app.kubernetes.io/component=price"
    echo "   2. Check pod status: kubectl describe pod -n real-estate -l app.kubernetes.io/component=price"
    echo "   3. Check events:     kubectl get events -n real-estate --sort-by='.lastTimestamp'"
    echo "   4. Check PostgreSQL: docker logs price-db"
    echo "   5. Check Kafka:      docker logs kafka"
    echo "   6. Test from pod:    kubectl exec -it deployment/real-estate-price-price -n real-estate -- sh"
    echo "                        Then: ping host.docker.internal"
    echo "                              nc -zv host.docker.internal 5435"
    echo "                              nc -zv host.docker.internal 29092"
    echo ""
    echo "   7. Manual port-forward:"
    echo "      kubectl port-forward svc/real-estate-price-price 8084:8084 -n real-estate &"
    echo "      curl http://localhost:8084/actuator/health"
fi
echo ""
