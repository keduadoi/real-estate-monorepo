#!/bin/bash
# Script to start analytics-service with external MongoDB + Kafka (Docker) + Service in Minikube
# This approach matches production architecture: DocumentDB + MSK + EKS

echo "🚀 Starting Analytics Service (External MongoDB + Kafka + Minikube)"
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

# Start MongoDB in Docker
echo "🍃 Starting MongoDB in Docker..."
if docker ps | grep -q "analytics-mongo"; then
    echo -e "${GREEN}✅ MongoDB is already running${NC}"
else
    docker-compose -f docker-compose.yml up -d mongo
    echo "⏳ Waiting for MongoDB to be ready..."
    sleep 5

    # Verify MongoDB is running
    if docker ps | grep -q "analytics-mongo"; then
        echo -e "${GREEN}✅ MongoDB started successfully${NC}"
    else
        echo -e "${RED}❌ Failed to start MongoDB${NC}"
        echo "   Check: docker ps"
        echo "   Logs:  docker logs analytics-mongo"
        exit 1
    fi
fi
echo ""

# Start Kafka in Docker
echo "📨 Starting Kafka in Docker..."
if docker ps | grep -q "analytics-kafka"; then
    echo -e "${GREEN}✅ Kafka is already running${NC}"
else
    docker-compose -f docker-compose.yml up -d kafka
    echo "⏳ Waiting for Kafka to be ready (this may take 30s)..."
    sleep 15

    # Wait for Kafka health check
    RETRIES=10
    while [ $RETRIES -gt 0 ]; do
        if docker exec analytics-kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1; then
            break
        fi
        RETRIES=$((RETRIES - 1))
        sleep 5
    done

    if docker ps | grep -q "analytics-kafka"; then
        echo -e "${GREEN}✅ Kafka started successfully${NC}"
    else
        echo -e "${RED}❌ Failed to start Kafka${NC}"
        echo "   Check: docker ps"
        echo "   Logs:  docker logs analytics-kafka"
        exit 1
    fi
fi
echo ""

# Check if we need to build the image
echo "🔨 Building Analytics Service Docker Image..."
# Point Docker to Minikube's daemon
eval $(minikube docker-env)

# Check if image exists in Minikube
if docker images | grep -q "analytics-service.*latest"; then
    echo -e "${YELLOW}ℹ️  Analytics service image already exists in Minikube${NC}"
    read -p "   Rebuild image? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "   Building new image..."
        mvn clean package -DskipTests
        docker build -t analytics-service:latest .
        echo -e "${GREEN}✅ Image rebuilt${NC}"
    fi
else
    echo "   Building analytics-service image..."
    mvn clean package -DskipTests
    docker build -t analytics-service:latest .
    echo -e "${GREEN}✅ Image built successfully${NC}"
fi
echo ""

# Deploy to Kubernetes
echo "☸️  Deploying Analytics Service to Kubernetes..."

# Create namespace if it doesn't exist
kubectl create namespace real-estate 2>/dev/null || true

if helm list -n real-estate 2>/dev/null | grep -q "real-estate-analytics"; then
    echo -e "${YELLOW}ℹ️  Helm release already exists. Upgrading...${NC}"
    helm upgrade real-estate-analytics ./helm/real-estate-analytics \
      -f ./helm/real-estate-analytics/values-local.yaml \
      -n real-estate
else
    echo "   Installing Helm release..."
    helm install real-estate-analytics ./helm/real-estate-analytics \
      -f ./helm/real-estate-analytics/values-local.yaml \
      -n real-estate
fi
echo ""

# Wait for pods to be ready
echo "⏳ Waiting for pods to be ready (this may take 1-2 minutes)..."
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/component=analytics \
  -n real-estate --timeout=120s 2>/dev/null

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Analytics service pod is ready${NC}"
else
    echo -e "${YELLOW}⚠️  Pod is taking longer than expected...${NC}"
    echo "   Current status:"
    kubectl get pods -n real-estate -l app.kubernetes.io/component=analytics
fi
echo ""

# Display pod status
echo "📊 Pod Status:"
kubectl get pods -n real-estate -l app.kubernetes.io/component=analytics
echo ""

# Test the connection via port-forward
echo "🔍 Testing connection..."

# Kill any existing port-forward on 8083
pkill -f "port-forward.*8083:8083" 2>/dev/null
sleep 1

# Start port-forward in background
kubectl port-forward svc/real-estate-analytics-analytics 8083:8083 -n real-estate > /dev/null 2>&1 &
PF_PID=$!
sleep 3

ANALYTICS_URL="http://localhost:8083"

if curl -s ${ANALYTICS_URL}/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Analytics Service is accessible${NC}"
    echo ""
    echo "🎉 Analytics services are ready!"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📍 Access URLs (via port-forward):"
    echo "   Analytics:     ${ANALYTICS_URL}"
    echo "   Health Check:  ${ANALYTICS_URL}/actuator/health"
    echo "   Metrics:       ${ANALYTICS_URL}/actuator/prometheus"
    echo ""
    echo "🔌 Port-forward is running (PID: ${PF_PID})"
    echo "   To stop: kill ${PF_PID}"
    echo "   To restart: kubectl port-forward svc/real-estate-analytics-analytics 8083:8083 -n real-estate &"
    echo ""
    echo "🍃 MongoDB Connection (Docker):"
    echo "   Host:     localhost"
    echo "   Port:     27017"
    echo "   Database: analyticsdb"
    echo "   Connect:  mongosh mongodb://localhost:27017/analyticsdb"
    echo ""
    echo "📨 Kafka (Docker):"
    echo "   Bootstrap: localhost:29092"
    echo "   Topics:    price-change-events, user-activity-events"
    echo ""
    echo "📦 Useful Commands:"
    echo "   Service logs:  kubectl logs -f deployment/real-estate-analytics-analytics -n real-estate"
    echo "   MongoDB logs:  docker logs -f analytics-mongo"
    echo "   Kafka logs:    docker logs -f analytics-kafka"
    echo "   View pods:     kubectl get pods -n real-estate"
    echo "   Status:        ./k8s-status-external.sh"
    echo "   Stop all:      ./k8s-stop-external.sh"
    echo ""
    echo "🔧 Rebuild and Redeploy:"
    echo "   eval \$(minikube docker-env)"
    echo "   mvn clean package -DskipTests"
    echo "   docker build -t analytics-service:latest ."
    echo "   kubectl rollout restart deployment/real-estate-analytics-analytics -n real-estate"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
else
    # Kill the failed port-forward
    kill $PF_PID 2>/dev/null

    echo -e "${RED}❌ Failed to connect to analytics service${NC}"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "   1. Check pod logs:   kubectl logs -n real-estate -l app.kubernetes.io/component=analytics"
    echo "   2. Check pod status: kubectl describe pod -n real-estate -l app.kubernetes.io/component=analytics"
    echo "   3. Check events:     kubectl get events -n real-estate --sort-by='.lastTimestamp'"
    echo "   4. Check MongoDB:    docker logs analytics-mongo"
    echo "   5. Check Kafka:      docker logs analytics-kafka"
    echo "   6. Test from pod:    kubectl exec -it deployment/real-estate-analytics-analytics -n real-estate -- sh"
    echo "                        Then: ping host.docker.internal"
    echo "                              nc -zv host.docker.internal 27017"
    echo "                              nc -zv host.docker.internal 29092"
    echo ""
    echo "   7. Manual port-forward:"
    echo "      kubectl port-forward svc/real-estate-analytics-analytics 8083:8083 -n real-estate &"
    echo "      curl http://localhost:8083/actuator/health"
fi
echo ""
