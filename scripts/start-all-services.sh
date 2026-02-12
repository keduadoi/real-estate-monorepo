#!/bin/bash
# Master script to start all Real Estate services
# Services: All microservices + databases via Docker Compose + Kong Gateway (Docker)

echo "🚀 Starting Real Estate Platform - All Services"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "📂 Project root: $ROOT_DIR"
echo ""

# ============================================================================
# STEP 1: Check Prerequisites
# ============================================================================
echo -e "${BLUE}━━━ Step 1: Checking Prerequisites ━━━${NC}"

# Check Docker
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker Desktop.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker is running${NC}"
echo ""

# ============================================================================
# STEP 2: Start Backend (Docker Compose)
# ============================================================================
echo -e "${BLUE}━━━ Step 2: Starting Property Service (Docker Compose) ━━━${NC}"
cd "$ROOT_DIR/property-service"

if docker ps --format '{{.Names}}' | grep -q "^property-service$"; then
    echo "   Property service is already running, rebuilding..."
fi
docker compose up -d --build
echo -e "${GREEN}✅ Property service started (DB: port 5432, App: port 8080)${NC}"
echo ""

# ============================================================================
# STEP 3: Start Auth Service (Docker Compose)
# ============================================================================
echo -e "${BLUE}━━━ Step 3: Starting Auth Service (Docker Compose) ━━━${NC}"
cd "$ROOT_DIR/auth-service"

if docker ps --format '{{.Names}}' | grep -q "^auth-service$"; then
    echo "   Auth service is already running, rebuilding..."
fi
docker compose up -d --build
echo -e "${GREEN}✅ Auth service started (DB: port 5433, App: port 8081)${NC}"
echo ""

# ============================================================================
# STEP 4: Start Post Service (Docker Compose)
# ============================================================================
echo -e "${BLUE}━━━ Step 4: Starting Post Service (Docker Compose) ━━━${NC}"
cd "$ROOT_DIR/post-service"

if docker ps --format '{{.Names}}' | grep -q "^post-service$"; then
    echo "   Post service is already running, rebuilding..."
fi
docker compose up -d --build
echo -e "${GREEN}✅ Post service started (DB: port 5434, App: port 8082)${NC}"
echo ""

# ============================================================================
# STEP 5: Start Analytics Service (Docker Compose + Kafka)
# ============================================================================
echo -e "${BLUE}━━━ Step 5: Starting Analytics Service (Docker Compose + Kafka) ━━━${NC}"
cd "$ROOT_DIR/analytics-service"

if docker ps --format '{{.Names}}' | grep -q "^analytics-service$"; then
    echo "   Analytics service is already running, rebuilding..."
fi
docker compose up -d --build
echo -e "${GREEN}✅ Analytics service started (Kafka: port 29092, App: port 8083)${NC}"
echo ""

# ============================================================================
# STEP 6: Start Kong Gateway (Docker Compose)
# ============================================================================
echo -e "${BLUE}━━━ Step 6: Starting Kong Gateway (Docker Compose) ━━━${NC}"
cd "$ROOT_DIR/kong"

if docker ps --filter "name=kong-gateway" --format '{{.Status}}' 2>/dev/null | grep -q "Up"; then
    echo "   Kong is already running, restarting..."
    docker compose down
fi
docker compose up -d

echo "   Waiting for Kong to be healthy..."
RETRY=0
while [ $RETRY -lt 30 ]; do
    if curl -s http://localhost:8001/status > /dev/null 2>&1; then
        break
    fi
    RETRY=$((RETRY + 1))
    sleep 2
done
if [ $RETRY -eq 30 ]; then
    echo -e "   ${RED}❌ Kong did not start in time${NC}"
else
    echo -e "${GREEN}✅ Kong Gateway running (Docker)${NC}"
fi
echo ""

# ============================================================================
# STEP 7: Health Checks
# ============================================================================
echo -e "${BLUE}━━━ Step 7: Health Checks ━━━${NC}"

check_health() {
    local name=$1
    local url=$2
    local retries=30
    local count=0

    while [ $count -lt $retries ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e "   ${GREEN}✅ $name is healthy${NC}"
            return 0
        fi
        count=$((count + 1))
        sleep 2
    done
    echo -e "   ${RED}❌ $name is not responding${NC}"
    return 1
}

echo "   Waiting for services to be ready (first build may take a few minutes)..."
check_health "Property Service" "http://localhost:8080/actuator/health"
check_health "Auth Service" "http://localhost:8081/actuator/health"
check_health "Post Service" "http://localhost:8082/actuator/health"
check_health "Analytics Service" "http://localhost:8083/actuator/health"
check_health "Kong Gateway" "http://localhost:8001/status"
echo ""

# ============================================================================
# STEP 8: Optional - Start Frontend
# ============================================================================
echo -e "${BLUE}━━━ Step 8: Frontend ━━━${NC}"
echo ""
read -p "Start frontend dev server? (y/N): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "   Starting frontend in new terminal..."

    # Check OS and open appropriate terminal
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        osascript -e "tell app \"Terminal\" to do script \"cd $ROOT_DIR/frontend && ./start-dev.sh\""
        echo -e "${GREEN}✅ Frontend started in new Terminal window${NC}"
    else
        # Linux - try common terminals
        if command -v gnome-terminal &> /dev/null; then
            gnome-terminal -- bash -c "cd $ROOT_DIR/frontend && ./start-dev.sh; bash"
        elif command -v xterm &> /dev/null; then
            xterm -e "cd $ROOT_DIR/frontend && ./start-dev.sh" &
        else
            echo -e "${YELLOW}⚠️  Could not open new terminal. Start manually:${NC}"
            echo "   cd $ROOT_DIR/frontend && ./start-dev.sh"
        fi
    fi
else
    echo -e "${YELLOW}ℹ️  Skipping frontend. Start manually when needed:${NC}"
    echo "   cd $ROOT_DIR/frontend && ./start-dev.sh"
fi
echo ""

# ============================================================================
# Summary
# ============================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}🎉 All backend services are running!${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📍 Service URLs:"
echo "   Frontend:      http://localhost:3000"
echo "   Kong Gateway:  http://localhost:8000"
echo "   Kong Admin:    http://localhost:8001"
echo "   Property API:  http://localhost:8080"
echo "   Auth Service:  http://localhost:8081"
echo "   Post Service:  http://localhost:8082"
echo "   Analytics Svc: http://localhost:8083"
echo ""
echo "🐘 Databases & Kafka (Docker):"
echo "   Property DB:   localhost:5432 (realestatedb)"
echo "   Auth DB:       localhost:5433 (authdb)"
echo "   Post DB:       localhost:5434 (postdb)"
echo "   Kafka:         localhost:29092"
echo ""
echo "🔧 Commands:"
echo "   Status:        $SCRIPT_DIR/status-all-services.sh"
echo "   Stop:          $SCRIPT_DIR/stop-all-services.sh"
echo "   Logs:          docker logs -f <container-name>"
echo "                  (property-service | auth-service | post-service | analytics-service | kong-gateway)"
echo ""
echo "🌐 Frontend Commands:"
echo "   Start:   cd $ROOT_DIR/frontend && ./start-dev.sh"
echo "   Status:  cd $ROOT_DIR/frontend && ./status.sh"
echo "   Stop:    cd $ROOT_DIR/frontend && ./stop.sh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
