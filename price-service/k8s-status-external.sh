#!/bin/bash
# Script to check status of price-service with external PostgreSQL + Kafka

echo "📊 Price Service Status (External PostgreSQL + Kafka)"
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

# Check PostgreSQL in Docker
echo "🐘 PostgreSQL (Docker):"
if docker ps | grep -q "price-db"; then
    echo -e "   ${GREEN}✅ Running${NC}"
    docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAMES|price-db"

    # Test connection
    if docker exec price-db pg_isready -U postgres > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Database accepting connections${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Database not ready${NC}"
    fi
else
    if docker ps -a | grep -q "price-db"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
        echo "   Start with: docker-compose -f docker-compose-db.yml up -d"
    else
        echo -e "   ${RED}❌ Not found${NC}"
        echo "   Start with: docker-compose -f docker-compose-db.yml up -d"
    fi
fi
echo ""

# Check Kafka in Docker
echo "📨 Kafka (Docker):"
if docker ps | grep -q "kafka"; then
    echo -e "   ${GREEN}✅ Running${NC}"
    docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAMES|kafka"

    # Test Kafka broker
    if docker exec kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Kafka broker is healthy${NC}"

        # List topics
        echo "   Topics:"
        docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null | while read topic; do
            echo "     - $topic"
        done
    else
        echo -e "   ${YELLOW}⚠️  Kafka broker not ready${NC}"
    fi
else
    if docker ps -a | grep -q "kafka"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
        echo "   Start with: docker-compose -f ../kafka/docker-compose.yml up -d"
    else
        echo -e "   ${RED}❌ Not found${NC}"
        echo "   Start with: docker-compose -f ../kafka/docker-compose.yml up -d"
    fi
fi
echo ""

# Check Helm release
echo "📦 Helm Release:"
if helm list -n real-estate 2>/dev/null | grep -q "real-estate-price"; then
    helm list -n real-estate | grep real-estate-price
else
    echo -e "   ${RED}❌ Not installed${NC}"
    echo "   Install with: ./k8s-start-external.sh"
fi
echo ""

# Check Kubernetes pods
echo "🐳 Kubernetes Pods:"
if kubectl get namespace real-estate > /dev/null 2>&1; then
    kubectl get pods -n real-estate -l app.kubernetes.io/component=price
    echo ""

    # Check if price pod is ready
    if kubectl get pods -n real-estate 2>/dev/null | grep -q "real-estate-price.*Running"; then
        echo -e "   ${GREEN}✅ Price pod is running${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Price pod not running${NC}"
    fi
else
    echo -e "   ${RED}❌ Namespace 'real-estate' not found${NC}"
    echo "   Deploy with: ./k8s-start-external.sh"
fi
echo ""

# Check Services
echo "🌐 Kubernetes Services:"
kubectl get svc -n real-estate -l app.kubernetes.io/component=price 2>/dev/null || echo "   No services found"
echo ""

# Check Persistent Volumes
echo "💾 Persistent Storage:"
echo "   Kubernetes PVCs:"
kubectl get pvc -n real-estate -l app.kubernetes.io/component=price 2>/dev/null || echo "      No PVCs found"
echo ""
echo "   Docker Volumes:"
docker volume ls | grep -E "NAME|price-db-data|kafka-data" || echo "      No volumes found"
echo ""

# Test price-service health via port-forward
echo "🏥 Price Service Health Check:"

# Check if price pod is running first
if kubectl get pods -n real-estate 2>/dev/null | grep -q "real-estate-price.*Running"; then
    # Kill any existing port-forward on test port
    pkill -f "port-forward.*18084:8084" 2>/dev/null
    sleep 1

    # Start temporary port-forward for health check
    kubectl port-forward svc/real-estate-price-price 18084:8084 -n real-estate > /dev/null 2>&1 &
    PF_PID=$!
    sleep 2

    PRICE_URL="http://localhost:18084"

    if curl -s ${PRICE_URL}/actuator/health > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Price service is healthy${NC}"
        HEALTH_RESPONSE=$(curl -s ${PRICE_URL}/actuator/health)
        HEALTH_STATUS=$(echo "$HEALTH_RESPONSE" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
        echo "   Status: ${HEALTH_STATUS}"

        # Check database connection
        DB_STATUS=$(echo "$HEALTH_RESPONSE" | grep -o '"db":{[^}]*}' | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$DB_STATUS" == "UP" ]; then
            echo -e "   ${GREEN}✅ Database connection: UP${NC}"
        else
            echo -e "   ${RED}❌ Database connection: ${DB_STATUS:-UNKNOWN}${NC}"
        fi

        # Check circuit breaker
        CB_RESPONSE=$(curl -s ${PRICE_URL}/actuator/circuitbreakers 2>/dev/null)
        if [ -n "$CB_RESPONSE" ]; then
            echo "   Circuit Breakers:"
            echo "   $CB_RESPONSE" | grep -o '"state":"[^"]*"' | while read cb; do
                echo "     $cb"
            done
        fi
    else
        echo -e "   ${RED}❌ Price service not accessible${NC}"
        echo ""
        echo "   Troubleshooting:"
        echo "   1. Check pod logs: kubectl logs -n real-estate -l app.kubernetes.io/component=price"
        echo "   2. Check service: kubectl get svc -n real-estate"
        echo "   3. Manual test: kubectl port-forward svc/real-estate-price-price 8084:8084 -n real-estate &"
    fi

    # Clean up temporary port-forward
    kill $PF_PID 2>/dev/null
else
    echo -e "   ${YELLOW}⏭️  Skipped (price pod not running)${NC}"
fi
echo ""

# Network connectivity check
echo "🔌 Network Connectivity:"
if kubectl get pods -n real-estate 2>/dev/null | grep -q "real-estate-price.*Running"; then
    echo "   Testing price-service -> infrastructure connections..."

    # Try host.docker.internal first (works on Mac/Windows with Docker Desktop)
    PING_RESULT=$(kubectl exec -n real-estate deployment/real-estate-price-price -- ping -c 1 host.docker.internal 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo -e "   ${GREEN}✅ Can reach host.docker.internal${NC}"
        INFRA_HOST="host.docker.internal"
    else
        PING_RESULT=$(kubectl exec -n real-estate deployment/real-estate-price-price -- ping -c 1 host.minikube.internal 2>/dev/null)
        if [ $? -eq 0 ]; then
            echo -e "   ${GREEN}✅ Can reach host.minikube.internal${NC}"
            INFRA_HOST="host.minikube.internal"
        else
            echo -e "   ${YELLOW}⚠️  Cannot ping host gateway${NC}"
            INFRA_HOST="host.docker.internal"
        fi
    fi

    # Test PostgreSQL port
    NC_RESULT=$(kubectl exec -n real-estate deployment/real-estate-price-price -- nc -zv ${INFRA_HOST} 5435 2>&1)
    if echo "$NC_RESULT" | grep -q "succeeded\|open"; then
        echo -e "   ${GREEN}✅ PostgreSQL port 5435 is reachable${NC}"
    else
        echo -e "   ${RED}❌ Cannot reach PostgreSQL on port 5435${NC}"
    fi

    # Test Kafka port
    NC_RESULT=$(kubectl exec -n real-estate deployment/real-estate-price-price -- nc -zv ${INFRA_HOST} 29092 2>&1)
    if echo "$NC_RESULT" | grep -q "succeeded\|open"; then
        echo -e "   ${GREEN}✅ Kafka port 29092 is reachable${NC}"
    else
        echo -e "   ${RED}❌ Cannot reach Kafka on port 29092${NC}"
    fi
else
    echo "   Skipped (price pod not running)"
fi
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Quick Access (via port-forward):"
echo "   REST API: http://localhost:8084"
echo "   Health:   http://localhost:8084/actuator/health"
echo "   Metrics:  http://localhost:8084/actuator/prometheus"
echo "   gRPC:     localhost:9090"
echo "   DB:       psql -h localhost -p 5435 -U postgres -d pricedb"
echo "   Kafka:    localhost:29092"
echo ""
echo "🔌 Start Port-Forwards:"
echo "   kubectl port-forward svc/real-estate-price-price 8084:8084 -n real-estate &"
echo "   kubectl port-forward svc/real-estate-price-price 9090:9090 -n real-estate &"
echo ""
echo "🔧 Quick Commands:"
echo "   Start all:      ./k8s-start-external.sh"
echo "   Stop all:       ./k8s-stop-external.sh"
echo "   Service logs:   kubectl logs -f deployment/real-estate-price-price -n real-estate"
echo "   DB logs:        docker logs -f price-db"
echo "   Kafka logs:     docker logs -f kafka"
echo "   Restart pod:    kubectl rollout restart deployment/real-estate-price-price -n real-estate"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
