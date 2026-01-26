#!/bin/bash
# Script to check status of backend with external PostgreSQL

echo "📊 Real Estate Backend Status (External PostgreSQL)"
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
if docker ps | grep -q "real-estate-postgres-local"; then
    echo -e "   ${GREEN}✅ Running${NC}"
    docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAMES|real-estate-postgres-local"

    # Test connection
    if docker exec real-estate-postgres-local pg_isready -U postgres > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Database accepting connections${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Database not ready${NC}"
    fi
else
    if docker ps -a | grep -q "real-estate-postgres-local"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
        echo "   Start with: docker-compose -f docker-compose-db.yml up -d"
    else
        echo -e "   ${RED}❌ Not found${NC}"
        echo "   Start with: docker-compose -f docker-compose-db.yml up -d"
    fi
fi
echo ""

# Check Helm release
echo "📦 Helm Release:"
if helm list -n real-estate 2>/dev/null | grep -q "real-estate"; then
    helm list -n real-estate
else
    echo -e "   ${RED}❌ Not installed${NC}"
    echo "   Install with: ./k8s-start-external.sh"
fi
echo ""

# Check Kubernetes pods
echo "🐳 Kubernetes Pods:"
if kubectl get namespace real-estate > /dev/null 2>&1; then
    kubectl get pods -n real-estate
    echo ""

    # Check if backend pod is ready
    if kubectl get pods -n real-estate 2>/dev/null | grep -q "real-estate-backend-backend.*Running"; then
        POD_STATUS=$(kubectl get pods -n real-estate -o jsonpath='{.items[?(@.metadata.labels.app\.kubernetes\.io/component=="backend")].status.phase}')
        if [ "$POD_STATUS" == "Running" ]; then
            echo -e "   ${GREEN}✅ Backend pod is running${NC}"
        fi
    else
        echo -e "   ${YELLOW}⚠️  Backend pod not running${NC}"
    fi
else
    echo -e "   ${RED}❌ Namespace 'real-estate' not found${NC}"
    echo "   Deploy with: ./k8s-start-external.sh"
fi
echo ""

# Check Services
echo "🌐 Kubernetes Services:"
kubectl get svc -n real-estate 2>/dev/null || echo "   No services found"
echo ""

# Check Persistent Volumes
echo "💾 Persistent Storage:"
echo "   Kubernetes PVCs:"
kubectl get pvc -n real-estate 2>/dev/null || echo "      No PVCs found"
echo ""
echo "   Docker Volumes:"
docker volume ls | grep -E "NAME|postgres_data" || echo "      No volumes found"
echo ""

# Get Minikube IP and test backend
if minikube status > /dev/null 2>&1; then
    MINIKUBE_IP=$(minikube ip)
    BACKEND_URL="http://${MINIKUBE_IP}:30080"

    echo "🏥 Backend Health Check:"
    if curl -s ${BACKEND_URL}/actuator/health > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Backend is healthy${NC}"
        HEALTH_STATUS=$(curl -s ${BACKEND_URL}/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        echo "   Status: ${HEALTH_STATUS}"

        # Check database connection
        DB_STATUS=$(curl -s ${BACKEND_URL}/actuator/health | grep -o '"db":{[^}]*}' | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$DB_STATUS" == "UP" ]; then
            echo -e "   ${GREEN}✅ Database connection: UP${NC}"
        else
            echo -e "   ${RED}❌ Database connection: ${DB_STATUS}${NC}"
        fi
    else
        echo -e "   ${RED}❌ Backend not accessible${NC}"
        echo "   Expected URL: ${BACKEND_URL}/actuator/health"
        echo ""
        echo "   Troubleshooting:"
        echo "   1. Check if pod is running: kubectl get pods -n real-estate"
        echo "   2. Check pod logs: kubectl logs -n real-estate -l app.kubernetes.io/component=backend"
        echo "   3. Check service: kubectl get svc -n real-estate"
    fi
    echo ""
fi

# Network connectivity check
echo "🔌 Network Connectivity:"
if kubectl get pods -n real-estate 2>/dev/null | grep -q "real-estate-backend-backend.*Running"; then
    echo "   Testing backend -> PostgreSQL connection..."
    PING_RESULT=$(kubectl exec -n real-estate deployment/real-estate-backend-backend -- ping -c 1 host.minikube.internal 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo -e "   ${GREEN}✅ Backend can reach host.minikube.internal${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Cannot ping host.minikube.internal${NC}"
    fi

    # Test PostgreSQL port
    NC_RESULT=$(kubectl exec -n real-estate deployment/real-estate-backend-backend -- nc -zv host.minikube.internal 5432 2>&1)
    if echo "$NC_RESULT" | grep -q "succeeded"; then
        echo -e "   ${GREEN}✅ PostgreSQL port 5432 is reachable${NC}"
    else
        echo -e "   ${RED}❌ Cannot reach PostgreSQL on port 5432${NC}"
    fi
else
    echo "   Skipped (backend pod not running)"
fi
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Quick Access:"
if minikube status > /dev/null 2>&1; then
    echo "   Backend:  ${BACKEND_URL}"
    echo "   Health:   ${BACKEND_URL}/actuator/health"
    echo "   Swagger:  ${BACKEND_URL}/swagger-ui.html"
fi
echo "   DB:       psql -h localhost -p 5432 -U postgres -d realestatedb"
echo ""
echo "🔧 Quick Commands:"
echo "   Start all:      ./k8s-start-external.sh"
echo "   Stop all:       ./k8s-stop-external.sh"
echo "   Backend logs:   kubectl logs -f deployment/real-estate-backend-backend -n real-estate"
echo "   DB logs:        docker logs -f real-estate-postgres-local"
echo "   Restart pod:    kubectl rollout restart deployment/real-estate-backend-backend -n real-estate"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
