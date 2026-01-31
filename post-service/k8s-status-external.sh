#!/bin/bash
# Script to check status of post-service with external PostgreSQL

echo "📊 Post Service Status (External PostgreSQL)"
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
else
    echo -e "   ${RED}❌ Not running${NC}"
    echo "   Start with: minikube start --memory=4096 --cpus=2"
fi
echo ""

# Check PostgreSQL in Docker
echo "🐘 PostgreSQL (Docker):"
if docker ps | grep -q "post-db"; then
    echo -e "   ${GREEN}✅ Running${NC}"
    docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAMES|post-db"

    if docker exec post-db pg_isready -U postgres > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Database accepting connections${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Database not ready${NC}"
    fi
else
    if docker ps -a | grep -q "post-db"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
    else
        echo -e "   ${RED}❌ Not found${NC}"
    fi
    echo "   Start with: docker-compose -f docker-compose-db.yml up -d"
fi
echo ""

# Check Helm release
echo "📦 Helm Release:"
if helm list -n real-estate 2>/dev/null | grep -q "post-service"; then
    helm list -n real-estate | grep -E "NAME|post-service"
else
    echo -e "   ${RED}❌ Not installed${NC}"
    echo "   Install with: ./k8s-start-external.sh"
fi
echo ""

# Check Kubernetes pods
echo "🐳 Kubernetes Pods:"
if kubectl get namespace real-estate > /dev/null 2>&1; then
    kubectl get pods -n real-estate -l app.kubernetes.io/name=post-service

    if kubectl get pods -n real-estate 2>/dev/null | grep -q "post-service.*Running"; then
        echo -e "   ${GREEN}✅ Post service pod is running${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Post service pod not running${NC}"
    fi
else
    echo -e "   ${RED}❌ Namespace 'real-estate' not found${NC}"
fi
echo ""

# Check Services
echo "🌐 Kubernetes Services:"
kubectl get svc -n real-estate -l app.kubernetes.io/name=post-service 2>/dev/null || echo "   No services found"
echo ""

# Health check via port-forward
echo "🏥 Service Health Check:"
if kubectl get pods -n real-estate 2>/dev/null | grep -q "post-service.*Running"; then
    pkill -f "port-forward.*18082:8082" 2>/dev/null
    sleep 1

    kubectl port-forward svc/post-service 18082:8082 -n real-estate > /dev/null 2>&1 &
    PF_PID=$!
    sleep 2

    SERVICE_URL="http://localhost:18082"

    if curl -s ${SERVICE_URL}/actuator/health > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Post service is healthy${NC}"
        HEALTH_RESPONSE=$(curl -s ${SERVICE_URL}/actuator/health)
        HEALTH_STATUS=$(echo "$HEALTH_RESPONSE" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
        echo "   Status: ${HEALTH_STATUS}"

        # Check database connection
        DB_STATUS=$(echo "$HEALTH_RESPONSE" | grep -o '"db":{[^}]*}' | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$DB_STATUS" == "UP" ]; then
            echo -e "   ${GREEN}✅ Database connection: UP${NC}"
        else
            echo -e "   ${RED}❌ Database connection: ${DB_STATUS:-UNKNOWN}${NC}"
        fi
    else
        echo -e "   ${RED}❌ Post service not accessible${NC}"
    fi

    kill $PF_PID 2>/dev/null
else
    echo -e "   ${YELLOW}⏭️  Skipped (post service pod not running)${NC}"
fi
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Quick Access (via port-forward):"
echo "   Post Service: http://localhost:8082"
echo "   Health:       http://localhost:8082/actuator/health"
echo "   API Docs:     http://localhost:8082/swagger-ui.html"
echo "   DB:           psql -h localhost -p 5434 -U postgres -d postdb"
echo ""
echo "🔌 Start Port-Forward:"
echo "   kubectl port-forward svc/post-service 8082:8082 -n real-estate &"
echo ""
echo "🔧 Quick Commands:"
echo "   Start:    ./k8s-start-external.sh"
echo "   Stop:     ./k8s-stop-external.sh"
echo "   Logs:     kubectl logs -f deployment/post-service -n real-estate"
echo "   DB logs:  docker logs -f post-db"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
