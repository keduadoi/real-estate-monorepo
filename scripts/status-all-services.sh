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
echo ""

# ============================================================================
# Database Status (Docker)
# ============================================================================
echo -e "${BLUE}━━━ Databases (Docker) ━━━${NC}"

check_db() {
    local name=$1
    local container=$2
    local port=$3

    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        if docker exec "$container" pg_isready -U postgres > /dev/null 2>&1; then
            echo -e "$name:  ${GREEN}✅ Running (port $port)${NC}"
        else
            echo -e "$name:  ${YELLOW}⚠️  Container up but DB not ready${NC}"
        fi
    else
        if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
            echo -e "$name:  ${YELLOW}⏸️  Stopped${NC}"
        else
            echo -e "$name:  ${RED}❌ Not found${NC}"
        fi
    fi
}

check_db "Property DB" "property-db" "5432"
check_db "Auth DB    " "auth-db" "5433"
check_db "Post DB    " "post-db" "5434"
check_db "Price DB   " "price-db" "5435"
check_db "News DB    " "news-db" "5436"
check_db "Comment DB " "comment-db" "5437"
echo ""

# ============================================================================
# Kafka Status (Docker)
# ============================================================================
echo -e "${BLUE}━━━ Kafka & MongoDB (Docker) ━━━${NC}"
if docker ps --format '{{.Names}}' | grep -q "^analytics-kafka$"; then
    KAFKA_STATUS=$(docker ps --filter "name=^analytics-kafka$" --format '{{.Status}}')
    echo -e "Kafka:      ${GREEN}✅ Running (port 29092)${NC} ($KAFKA_STATUS)"
else
    if docker ps -a --format '{{.Names}}' | grep -q "^analytics-kafka$"; then
        echo -e "Kafka:      ${YELLOW}⏸️  Stopped${NC}"
    else
        echo -e "Kafka:      ${RED}❌ Not found${NC}"
    fi
fi
if docker ps --format '{{.Names}}' | grep -q "^analytics-mongo$"; then
    MONGO_STATUS=$(docker ps --filter "name=^analytics-mongo$" --format '{{.Status}}')
    echo -e "MongoDB:    ${GREEN}✅ Running (port 27017)${NC} ($MONGO_STATUS)"
else
    if docker ps -a --format '{{.Names}}' | grep -q "^analytics-mongo$"; then
        echo -e "MongoDB:    ${YELLOW}⏸️  Stopped${NC}"
    else
        echo -e "MongoDB:    ${RED}❌ Not found${NC}"
    fi
fi
echo ""

# ============================================================================
# App Services Status (Docker)
# ============================================================================
echo -e "${BLUE}━━━ App Services (Docker) ━━━${NC}"

check_service() {
    local name=$1
    local container=$2
    local port=$3

    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        local status=$(docker ps --filter "name=^${container}$" --format '{{.Status}}')
        echo -e "$name:  ${GREEN}✅ Running${NC} ($status)"
    else
        if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
            echo -e "$name:  ${YELLOW}⏸️  Stopped${NC}"
        else
            echo -e "$name:  ${RED}❌ Not found${NC}"
        fi
    fi
}

check_service "Property   " "property-service" "8080"
check_service "Auth       " "auth-service" "8081"
check_service "Post       " "post-service" "8082"
check_service "Analytics  " "analytics-service" "8083"
check_service "Price      " "price-service" "8084"
check_service "News       " "news-service" "8085"
check_service "AI Search  " "ai-search-service" "8086"
check_service "Comment    " "comment-service" "8087"
echo ""

# ============================================================================
# Kong Gateway Status (Docker)
# ============================================================================
echo -e "${BLUE}━━━ Kong Gateway (Docker) ━━━${NC}"
if docker ps --filter "name=kong-gateway" --format '{{.Status}}' 2>/dev/null | grep -q "Up"; then
    KONG_STATUS=$(docker ps --filter "name=kong-gateway" --format '{{.Status}}')
    echo -e "Container:  ${GREEN}Running${NC} ($KONG_STATUS)"
    if curl -s http://localhost:8001/status > /dev/null 2>&1; then
        SERVICES=$(curl -s http://localhost:8001/services | grep -o '"total":[0-9]*' | cut -d: -f2)
        ROUTES=$(curl -s http://localhost:8001/routes | grep -o '"total":[0-9]*' | cut -d: -f2)
        echo -e "Admin API:  ${GREEN}Healthy${NC} (services: ${SERVICES:-0}, routes: ${ROUTES:-0})"
    else
        echo -e "Admin API:  ${RED}Not responding${NC}"
    fi
else
    echo -e "Container:  ${RED}Not running${NC}"
fi
echo ""

# ============================================================================
# Service Health Checks
# ============================================================================
echo -e "${BLUE}━━━ Service Health ━━━${NC}"

check_health() {
    local name=$1
    local url=$2

    if curl -s "$url" > /dev/null 2>&1; then
        local status=$(curl -s "$url" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
        echo -e "$name:  ${GREEN}✅ Healthy${NC} (status: ${status:-OK})"
    else
        echo -e "$name:  ${RED}❌ Not responding${NC}"
    fi
}

check_health "Property Svc " "http://localhost:8080/actuator/health"
check_health "Auth Service " "http://localhost:8081/actuator/health"
check_health "Post Service " "http://localhost:8082/actuator/health"
check_health "Analytics Svc" "http://localhost:8083/actuator/health"
check_health "Price Service" "http://localhost:8084/actuator/health"
check_health "News Service " "http://localhost:8085/actuator/health"
check_health "AI Search Svc" "http://localhost:8086/actuator/health"
check_health "Comment Svc  " "http://localhost:8087/actuator/health"
# Kong health (Docker — direct access)
if curl -s http://localhost:8001/status > /dev/null 2>&1; then
    echo -e "Kong Gateway:  ${GREEN}✅ Healthy${NC}"
else
    echo -e "Kong Gateway:  ${RED}❌ Not responding${NC}"
fi
echo ""

# ============================================================================
# Frontend Status
# ============================================================================
echo -e "${BLUE}━━━ Frontend ━━━${NC}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

if [ -f "$ROOT_DIR/frontend/.frontend.pid" ] && kill -0 "$(cat "$ROOT_DIR/frontend/.frontend.pid")" 2>/dev/null; then
    echo -e "Next.js:    ${GREEN}✅ Running (PID: $(cat "$ROOT_DIR/frontend/.frontend.pid"))${NC}"
elif lsof -ti:3000 > /dev/null 2>&1; then
    echo -e "Next.js:    ${GREEN}✅ Running (port 3000)${NC}"
else
    echo -e "Next.js:    ${RED}❌ Not running${NC}"
fi

if curl -s http://localhost:3000 > /dev/null 2>&1; then
    echo -e "Health:     ${GREEN}✅ Responding at http://localhost:3000${NC}"
else
    echo -e "Health:     ${RED}❌ Not responding${NC}"
fi
echo ""

# ============================================================================
# Summary
# ============================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Service URLs:"
echo "   Frontend:      http://localhost:3000"
echo "   Kong Gateway:  http://localhost:8000"
echo "   Property Svc:  http://localhost:8080"
echo "   Auth Service:  http://localhost:8081"
echo "   Post Service:  http://localhost:8082"
echo "   Analytics Svc: http://localhost:8083"
echo "   Price Service: http://localhost:8084 (gRPC: 9090)"
echo "   News Service:  http://localhost:8085"
echo "   AI Search:     http://localhost:8086"
echo "   Comment Svc:   http://localhost:8087"
echo ""
echo "🔧 Commands:"
echo "   Start all:  ./scripts/start-all-services.sh"
echo "   Stop all:   ./scripts/stop-all-services.sh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
