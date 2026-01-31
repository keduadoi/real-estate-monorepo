#!/bin/bash
# Master script to check status of all Real Estate services

echo "📊 Real Estate Platform - Service Status"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ============================================================================
# Infrastructure Status
# ============================================================================
echo -e "${BLUE}━━━ Infrastructure ━━━${NC}"

# Docker
if docker info > /dev/null 2>&1; then
    echo -e "Docker:     ${GREEN}✅ Running${NC}"
else
    echo -e "Docker:     ${RED}❌ Not running${NC}"
fi

# Minikube
if minikube status > /dev/null 2>&1; then
    echo -e "Minikube:   ${GREEN}✅ Running${NC}"
else
    echo -e "Minikube:   ${RED}❌ Not running${NC}"
fi
echo ""

# ============================================================================
# Database Status (Docker)
# ============================================================================
echo -e "${BLUE}━━━ Databases (Docker) ━━━${NC}"

check_db() {
    local name=$1
    local container=$2
    local port=$3

    if docker ps | grep -q "$container"; then
        if docker exec "$container" pg_isready -U postgres > /dev/null 2>&1; then
            echo -e "$name:  ${GREEN}✅ Running (port $port)${NC}"
        else
            echo -e "$name:  ${YELLOW}⚠️  Container up but DB not ready${NC}"
        fi
    else
        if docker ps -a | grep -q "$container"; then
            echo -e "$name:  ${YELLOW}⏸️  Stopped${NC}"
        else
            echo -e "$name:  ${RED}❌ Not found${NC}"
        fi
    fi
}

check_db "Backend DB " "real-estate-postgres-local" "5432"
check_db "Auth DB    " "auth-db" "5433"
check_db "Post DB    " "post-db" "5434"
echo ""

# ============================================================================
# Kubernetes Pods Status
# ============================================================================
echo -e "${BLUE}━━━ Kubernetes Pods ━━━${NC}"
echo ""
echo "real-estate namespace:"
kubectl get pods -n real-estate 2>/dev/null || echo "  Namespace not found"
echo ""
echo "kong namespace:"
kubectl get pods -n kong 2>/dev/null || echo "  Namespace not found"
echo ""

# ============================================================================
# Helm Releases
# ============================================================================
echo -e "${BLUE}━━━ Helm Releases ━━━${NC}"
helm list -A 2>/dev/null | grep -E "NAME|real-estate|auth-service|post-service|kong"
echo ""

# ============================================================================
# Service Health Checks
# ============================================================================
echo -e "${BLUE}━━━ Service Health (via port-forward) ━━━${NC}"

check_health() {
    local name=$1
    local port=$2
    local path=$3
    local temp_port=$((port + 10000))

    # Check if service pod is running
    if ! kubectl get pods -A 2>/dev/null | grep -q "$name.*Running"; then
        echo -e "$name:  ${YELLOW}⏭️  Pod not running${NC}"
        return
    fi

    # Try to check health
    pkill -f "port-forward.*$temp_port:$port" 2>/dev/null

    # Determine namespace
    local ns="real-estate"
    if [[ "$name" == "kong"* ]]; then
        ns="kong"
    fi

    kubectl port-forward svc/$name $temp_port:$port -n $ns > /dev/null 2>&1 &
    local pf_pid=$!
    sleep 2

    if curl -s "http://localhost:$temp_port$path" > /dev/null 2>&1; then
        local status=$(curl -s "http://localhost:$temp_port$path" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
        echo -e "$name:  ${GREEN}✅ Healthy${NC} (status: ${status:-OK})"
    else
        echo -e "$name:  ${RED}❌ Not responding${NC}"
    fi

    kill $pf_pid 2>/dev/null
}

check_health "real-estate-backend-backend" "8080" "/actuator/health"
check_health "auth-service" "8081" "/actuator/health"
check_health "post-service" "8082" "/actuator/health"
check_health "kong-proxy" "80" "/"
echo ""

# ============================================================================
# Port Forwards Status
# ============================================================================
echo -e "${BLUE}━━━ Active Port Forwards ━━━${NC}"
ps aux | grep "port-forward" | grep -v grep | awk '{print "  " $NF}' || echo "  None active"
echo ""

# ============================================================================
# Summary
# ============================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Service URLs (when port-forwards are active):"
echo "   Kong Gateway:  http://localhost:8000"
echo "   Backend:       http://localhost:8080"
echo "   Auth Service:  http://localhost:8081"
echo "   Post Service:  http://localhost:8082"
echo ""
echo "🔧 Commands:"
echo "   Start all:  ./scripts/start-all-services.sh"
echo "   Stop all:   ./scripts/stop-all-services.sh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
