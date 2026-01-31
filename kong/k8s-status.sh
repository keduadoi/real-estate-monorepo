#!/bin/bash
# Script to check status of Kong API Gateway

echo "📊 Kong API Gateway Status"
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

# Check Helm release
echo "📦 Helm Release:"
if helm list -n kong 2>/dev/null | grep -q "kong-gateway"; then
    helm list -n kong | grep -E "NAME|kong-gateway"
else
    echo -e "   ${RED}❌ Not installed${NC}"
    echo "   Install with: ./k8s-start.sh"
fi
echo ""

# Check Kubernetes pods
echo "🐳 Kubernetes Pods:"
if kubectl get namespace kong > /dev/null 2>&1; then
    kubectl get pods -n kong

    # Check individual components
    echo ""
    if kubectl get pods -n kong 2>/dev/null | grep -q "kong-postgres.*Running"; then
        echo -e "   ${GREEN}✅ Kong PostgreSQL is running${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Kong PostgreSQL not running${NC}"
    fi

    if kubectl get pods -n kong 2>/dev/null | grep -q "kong-redis.*Running"; then
        echo -e "   ${GREEN}✅ Kong Redis is running${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Kong Redis not running${NC}"
    fi

    if kubectl get pods -n kong 2>/dev/null | grep -qE "kong-[a-z0-9]+-[a-z0-9]+.*Running" | grep -v postgres | grep -v redis; then
        echo -e "   ${GREEN}✅ Kong Gateway is running${NC}"
    fi
else
    echo -e "   ${RED}❌ Namespace 'kong' not found${NC}"
fi
echo ""

# Check Services
echo "🌐 Kubernetes Services:"
kubectl get svc -n kong 2>/dev/null || echo "   No services found"
echo ""

# Health check via port-forward
echo "🏥 Gateway Health Check:"
if kubectl get pods -n kong 2>/dev/null | grep -qE "^kong-[a-z0-9]+-[a-z0-9]+.*Running"; then
    pkill -f "port-forward.*18001:8001" 2>/dev/null
    sleep 1

    kubectl port-forward svc/kong-admin 18001:8001 -n kong > /dev/null 2>&1 &
    PF_PID=$!
    sleep 2

    if curl -s http://localhost:18001/status > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Kong Admin API is healthy${NC}"

        # Get some stats
        STATUS=$(curl -s http://localhost:18001/status)
        DB_STATUS=$(echo "$STATUS" | grep -o '"database":{[^}]*}' | grep -o '"reachable":[^,]*' | cut -d':' -f2)
        echo "   Database reachable: ${DB_STATUS:-unknown}"

        # Count routes and services
        ROUTES=$(curl -s http://localhost:18001/routes | grep -o '"total":[0-9]*' | cut -d':' -f2)
        SERVICES=$(curl -s http://localhost:18001/services | grep -o '"total":[0-9]*' | cut -d':' -f2)
        echo "   Routes configured: ${ROUTES:-0}"
        echo "   Services configured: ${SERVICES:-0}"
    else
        echo -e "   ${RED}❌ Kong Admin API not accessible${NC}"
    fi

    kill $PF_PID 2>/dev/null
else
    echo -e "   ${YELLOW}⏭️  Skipped (Kong pod not running)${NC}"
fi
echo ""

# Test proxy
echo "🔗 Proxy Test:"
pkill -f "port-forward.*18000:80" 2>/dev/null
kubectl port-forward svc/kong-proxy 18000:80 -n kong > /dev/null 2>&1 &
PF_PID=$!
sleep 2

if curl -s -o /dev/null -w "%{http_code}" http://localhost:18000 2>/dev/null | grep -qE "404|200"; then
    echo -e "   ${GREEN}✅ Kong Proxy is responding${NC}"
else
    echo -e "   ${RED}❌ Kong Proxy not responding${NC}"
fi
kill $PF_PID 2>/dev/null
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Quick Access (via port-forward):"
echo "   Kong Proxy:  http://localhost:8000"
echo "   Kong Admin:  http://localhost:8001"
echo ""
echo "🔌 Start Port-Forwards:"
echo "   kubectl port-forward svc/kong-proxy 8000:80 -n kong &"
echo "   kubectl port-forward svc/kong-admin 8001:8001 -n kong &"
echo ""
echo "🔧 Quick Commands:"
echo "   Start:       ./k8s-start.sh"
echo "   Stop:        ./k8s-stop.sh"
echo "   Logs:        kubectl logs -f deployment/kong -n kong"
echo "   Routes:      curl http://localhost:8001/routes | jq"
echo "   Services:    curl http://localhost:8001/services | jq"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
