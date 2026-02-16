#!/bin/bash
# Script to check status of analytics-service with external MongoDB + Kafka

echo "📊 Analytics Service Status (External MongoDB + Kafka)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check Minikube
echo "🖥️  Minikube Status:"
if minikube status > /dev/null 2>&1; then
    echo -e "   ${GREEN}✅ Running${NC}"
    minikube status | grep -E "host|kubelet|apiserver"
else
    echo -e "   ${RED}❌ Not running${NC}"
    echo "   Start with: minikube start --memory=3500 --cpus=2"
fi
echo ""

# Check MongoDB in Docker
echo "🍃 MongoDB (Docker):"
if docker ps | grep -q "analytics-mongo"; then
    echo -e "   ${GREEN}✅ Running${NC}"
    docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAMES|analytics-mongo"

    # Test connection
    if docker exec analytics-mongo mongosh --eval "db.adminCommand('ping')" > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ MongoDB accepting connections${NC}"
    else
        echo -e "   ${YELLOW}⚠️  MongoDB not ready${NC}"
    fi
else
    if docker ps -a | grep -q "analytics-mongo"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
        echo "   Start with: docker-compose -f docker-compose.yml up -d mongo"
    else
        echo -e "   ${RED}❌ Not found${NC}"
        echo "   Start with: docker-compose -f docker-compose.yml up -d mongo"
    fi
fi
echo ""

# Check Kafka in Docker
echo "📨 Kafka (Docker):"
if docker ps | grep -q "analytics-kafka"; then
    echo -e "   ${GREEN}✅ Running${NC}"
    docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAMES|analytics-kafka"

    # Test Kafka broker
    if docker exec analytics-kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Kafka broker is healthy${NC}"

        # List topics
        echo "   Topics:"
        docker exec analytics-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null | while read topic; do
            echo "     - $topic"
        done
    else
        echo -e "   ${YELLOW}⚠️  Kafka broker not ready${NC}"
    fi
else
    if docker ps -a | grep -q "analytics-kafka"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
        echo "   Start with: docker-compose -f docker-compose.yml up -d kafka"
    else
        echo -e "   ${RED}❌ Not found${NC}"
        echo "   Start with: docker-compose -f docker-compose.yml up -d kafka"
    fi
fi
echo ""

# Check Helm release
echo "📦 Helm Release:"
if helm list -n real-estate 2>/dev/null | grep -q "real-estate-analytics"; then
    helm list -n real-estate | grep real-estate-analytics
else
    echo -e "   ${RED}❌ Not installed${NC}"
    echo "   Install with: ./k8s-start-external.sh"
fi
echo ""

# Check Kubernetes pods
echo "🐳 Kubernetes Pods:"
if kubectl get namespace real-estate > /dev/null 2>&1; then
    kubectl get pods -n real-estate -l app.kubernetes.io/component=analytics
    echo ""

    # Check if analytics pod is ready
    if kubectl get pods -n real-estate 2>/dev/null | grep -q "real-estate-analytics.*Running"; then
        echo -e "   ${GREEN}✅ Analytics pod is running${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Analytics pod not running${NC}"
    fi
else
    echo -e "   ${RED}❌ Namespace 'real-estate' not found${NC}"
    echo "   Deploy with: ./k8s-start-external.sh"
fi
echo ""

# Check Services
echo "🌐 Kubernetes Services:"
kubectl get svc -n real-estate -l app.kubernetes.io/component=analytics 2>/dev/null || echo "   No services found"
echo ""

# Check Persistent Volumes
echo "💾 Persistent Storage:"
echo "   Docker Volumes:"
docker volume ls | grep -E "NAME|mongo-data|kafka-data" || echo "      No volumes found"
echo ""

# Test analytics health via port-forward
echo "🏥 Analytics Health Check:"

# Check if analytics pod is running first
if kubectl get pods -n real-estate 2>/dev/null | grep -q "real-estate-analytics.*Running"; then
    # Kill any existing port-forward on test port
    pkill -f "port-forward.*18083:8083" 2>/dev/null
    sleep 1

    # Start temporary port-forward for health check
    kubectl port-forward svc/real-estate-analytics-analytics 18083:8083 -n real-estate > /dev/null 2>&1 &
    PF_PID=$!
    sleep 2

    ANALYTICS_URL="http://localhost:18083"

    if curl -s ${ANALYTICS_URL}/actuator/health > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Analytics service is healthy${NC}"
        HEALTH_RESPONSE=$(curl -s ${ANALYTICS_URL}/actuator/health)
        HEALTH_STATUS=$(echo "$HEALTH_RESPONSE" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
        echo "   Status: ${HEALTH_STATUS}"

        # Check MongoDB connection
        MONGO_STATUS=$(echo "$HEALTH_RESPONSE" | grep -o '"mongo":{[^}]*}' | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$MONGO_STATUS" == "UP" ]; then
            echo -e "   ${GREEN}✅ MongoDB connection: UP${NC}"
        else
            echo -e "   ${RED}❌ MongoDB connection: ${MONGO_STATUS:-UNKNOWN}${NC}"
        fi
    else
        echo -e "   ${RED}❌ Analytics service not accessible${NC}"
        echo ""
        echo "   Troubleshooting:"
        echo "   1. Check pod logs: kubectl logs -n real-estate -l app.kubernetes.io/component=analytics"
        echo "   2. Check service: kubectl get svc -n real-estate"
        echo "   3. Manual test: kubectl port-forward svc/real-estate-analytics-analytics 8083:8083 -n real-estate &"
    fi

    # Clean up temporary port-forward
    kill $PF_PID 2>/dev/null
else
    echo -e "   ${YELLOW}⏭️  Skipped (analytics pod not running)${NC}"
fi
echo ""

# Network connectivity check
echo "🔌 Network Connectivity:"
if kubectl get pods -n real-estate 2>/dev/null | grep -q "real-estate-analytics.*Running"; then
    echo "   Testing analytics -> MongoDB connection..."

    # Try host.docker.internal first (works on Mac/Windows with Docker Desktop)
    PING_RESULT=$(kubectl exec -n real-estate deployment/real-estate-analytics-analytics -- ping -c 1 host.docker.internal 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo -e "   ${GREEN}✅ Can reach host.docker.internal${NC}"
        INFRA_HOST="host.docker.internal"
    else
        PING_RESULT=$(kubectl exec -n real-estate deployment/real-estate-analytics-analytics -- ping -c 1 host.minikube.internal 2>/dev/null)
        if [ $? -eq 0 ]; then
            echo -e "   ${GREEN}✅ Can reach host.minikube.internal${NC}"
            INFRA_HOST="host.minikube.internal"
        else
            echo -e "   ${YELLOW}⚠️  Cannot ping host gateway${NC}"
            INFRA_HOST="host.docker.internal"
        fi
    fi

    # Test MongoDB port
    NC_RESULT=$(kubectl exec -n real-estate deployment/real-estate-analytics-analytics -- nc -zv ${INFRA_HOST} 27017 2>&1)
    if echo "$NC_RESULT" | grep -q "succeeded\|open"; then
        echo -e "   ${GREEN}✅ MongoDB port 27017 is reachable${NC}"
    else
        echo -e "   ${RED}❌ Cannot reach MongoDB on port 27017${NC}"
    fi

    # Test Kafka port
    NC_RESULT=$(kubectl exec -n real-estate deployment/real-estate-analytics-analytics -- nc -zv ${INFRA_HOST} 29092 2>&1)
    if echo "$NC_RESULT" | grep -q "succeeded\|open"; then
        echo -e "   ${GREEN}✅ Kafka port 29092 is reachable${NC}"
    else
        echo -e "   ${RED}❌ Cannot reach Kafka on port 29092${NC}"
    fi
else
    echo "   Skipped (analytics pod not running)"
fi
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Quick Access (via port-forward):"
echo "   Analytics: http://localhost:8083"
echo "   Health:    http://localhost:8083/actuator/health"
echo "   Metrics:   http://localhost:8083/actuator/prometheus"
echo "   MongoDB:   mongosh mongodb://localhost:27017/analyticsdb"
echo "   Kafka:     localhost:29092"
echo ""
echo "🔌 Start Port-Forward:"
echo "   kubectl port-forward svc/real-estate-analytics-analytics 8083:8083 -n real-estate &"
echo ""
echo "🔧 Quick Commands:"
echo "   Start all:      ./k8s-start-external.sh"
echo "   Stop all:       ./k8s-stop-external.sh"
echo "   Service logs:   kubectl logs -f deployment/real-estate-analytics-analytics -n real-estate"
echo "   MongoDB logs:   docker logs -f analytics-mongo"
echo "   Kafka logs:     docker logs -f analytics-kafka"
echo "   Restart pod:    kubectl rollout restart deployment/real-estate-analytics-analytics -n real-estate"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
