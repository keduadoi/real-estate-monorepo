#!/bin/bash
# Master script to start all Real Estate services
# Services: All microservices + databases via Docker Compose + Kong Gateway (Docker) + Frontend
#
# Usage:
#   ./start-all-services.sh                # Start everything (backend + frontend)
#   ./start-all-services.sh --no-frontend  # Start backend only, skip frontend

echo "🚀 Starting Real Estate Platform - All Services"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Parse arguments
SKIP_FRONTEND=false
for arg in "$@"; do
    case $arg in
        --no-frontend) SKIP_FRONTEND=true ;;
    esac
done

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
echo -e "${GREEN}✅ Property service started (DB: port 5432, Redis: port 6379, App: port 8080)${NC}"
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
echo -e "${GREEN}✅ Analytics service started (Kafka: port 29092, MongoDB: port 27017, App: port 8083)${NC}"
echo ""

# ============================================================================
# STEP 6: Start Price Service (Docker Compose)
# ============================================================================
echo -e "${BLUE}━━━ Step 6: Starting Price Service (Docker Compose) ━━━${NC}"
cd "$ROOT_DIR/price-service"

if docker ps --format '{{.Names}}' | grep -q "^price-service$"; then
    echo "   Price service is already running, rebuilding..."
fi
docker compose up -d --build
echo -e "${GREEN}✅ Price service started (DB: port 5435, App: port 8084, gRPC: 9090)${NC}"
echo ""

# ============================================================================
# STEP 7: Start News Service (Docker Compose)
# ============================================================================
echo -e "${BLUE}━━━ Step 7: Starting News Service (Docker Compose) ━━━${NC}"
cd "$ROOT_DIR/news-service"

if docker ps --format '{{.Names}}' | grep -q "^news-service$"; then
    echo "   News service is already running, rebuilding..."
fi
docker compose up -d --build
echo -e "${GREEN}✅ News service started (DB: port 5436, App: port 8085)${NC}"
echo ""

# ============================================================================
# STEP 8: Start AI Search Service (Docker Compose)
# ============================================================================
echo -e "${BLUE}━━━ Step 8: Starting AI Search Service (Docker Compose) ━━━${NC}"
cd "$ROOT_DIR/ai-search-service"

if docker ps --format '{{.Names}}' | grep -q "^ai-search-service$"; then
    echo "   AI search service is already running, rebuilding..."
fi
docker compose up -d --build
echo -e "${GREEN}✅ AI search service started (App: port 8086)${NC}"
echo "   Mode: ${AI_SEARCH_PARSER_MODE:-regex}  (override with AI_SEARCH_PARSER_MODE=llm)"
if [ -z "$ANTHROPIC_API_KEY" ] && [ "${AI_SEARCH_PARSER_MODE:-regex}" = "llm" ]; then
    echo -e "   ${YELLOW}⚠️  ANTHROPIC_API_KEY not set; LLM parser calls will fail.${NC}"
fi
echo ""

# ============================================================================
# STEP 9: Start Comment Service (Docker Compose)
# ============================================================================
echo -e "${BLUE}━━━ Step 9: Starting Comment Service (Docker Compose) ━━━${NC}"
cd "$ROOT_DIR/comment-service"

if docker ps --format '{{.Names}}' | grep -q "^comment-service$"; then
    echo "   Comment service is already running, rebuilding..."
fi
docker compose up -d --build
echo -e "${GREEN}✅ Comment service started (DB: port 5437, App: port 8087)${NC}"
echo ""

# ============================================================================
# STEP 10: Start Kong Gateway (Docker Compose)
# ============================================================================
echo -e "${BLUE}━━━ Step 10: Starting Kong Gateway (Docker Compose) ━━━${NC}"
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
# STEP 11: Health Checks
# ============================================================================
echo -e "${BLUE}━━━ Step 11: Health Checks ━━━${NC}"

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
check_health "Price Service" "http://localhost:8084/actuator/health"
check_health "News Service" "http://localhost:8085/actuator/health"
check_health "AI Search Svc" "http://localhost:8086/actuator/health"
check_health "Comment Svc" "http://localhost:8087/actuator/health"
check_health "Kong Gateway" "http://localhost:8001/status"
echo ""

# ============================================================================
# STEP 12: Start Frontend
# ============================================================================
echo -e "${BLUE}━━━ Step 12: Frontend ━━━${NC}"

if [ "$SKIP_FRONTEND" = true ]; then
    echo -e "${YELLOW}ℹ️  Skipping frontend (--no-frontend flag).${NC}"
    echo "   Start manually: cd $ROOT_DIR/frontend && ./start-dev.sh"
else
    # Check Node.js
    if ! command -v node &> /dev/null; then
        echo -e "${RED}❌ Node.js not found. Skipping frontend.${NC}"
        echo "   Install from: https://nodejs.org/"
    else
        cd "$ROOT_DIR/frontend"

        # Install dependencies if needed
        if [ ! -d "node_modules" ]; then
            echo "   Installing frontend dependencies..."
            npm install --silent
        fi

        # Create .env.local if missing
        if [ ! -f ".env.local" ]; then
            echo "   Creating default .env.local..."
            cat > .env.local << 'ENVEOF'
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-secret-key-here-change-in-production-min-32-characters-long
NEXT_PUBLIC_KONG_URL=http://127.0.0.1:8000
NEXT_PUBLIC_API_URL=http://127.0.0.1:8000/api
NEXT_PUBLIC_AUTH_API_URL=http://127.0.0.1:8000
NEXT_PUBLIC_MAX_IMAGE_SIZE=10485760
NEXT_PUBLIC_MAX_IMAGES_PER_PROPERTY=10
ENVEOF
        fi

        # Kill any existing Next.js dev server on port 3000
        lsof -ti:3000 | xargs kill -9 2>/dev/null

        # Start frontend in background
        echo "   Starting Next.js dev server on port 3000..."
        nohup npm run dev > "$ROOT_DIR/frontend/.frontend.log" 2>&1 &
        FRONTEND_PID=$!
        echo "$FRONTEND_PID" > "$ROOT_DIR/frontend/.frontend.pid"

        # Wait for frontend to be ready
        RETRY=0
        while [ $RETRY -lt 30 ]; do
            if curl -s http://localhost:3000 > /dev/null 2>&1; then
                echo -e "   ${GREEN}✅ Frontend running at http://localhost:3000 (PID: $FRONTEND_PID)${NC}"
                break
            fi
            RETRY=$((RETRY + 1))
            sleep 2
        done
        if [ $RETRY -eq 30 ]; then
            echo -e "   ${YELLOW}⚠️  Frontend may still be starting. Check logs:${NC}"
            echo "      tail -f $ROOT_DIR/frontend/.frontend.log"
        fi
    fi
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
echo "   Price Service: http://localhost:8084 (gRPC: 9090)"
echo "   News Service:  http://localhost:8085"
echo "   AI Search:     http://localhost:8086"
echo "   Comment Svc:   http://localhost:8087"
echo ""
echo "🐘 Databases, Cache, Kafka & MongoDB (Docker):"
echo "   Property DB:   localhost:5432 (realestatedb)"
echo "   Auth DB:       localhost:5433 (authdb)"
echo "   Post DB:       localhost:5434 (postdb)"
echo "   Price DB:      localhost:5435 (pricedb)"
echo "   News DB:       localhost:5436 (newsdb)"
echo "   Comment DB:    localhost:5437 (commentsdb)"
echo "   Redis:         localhost:6379 (property-service cache)"
echo "   Kafka:         localhost:29092"
echo "   MongoDB:       localhost:27017 (analyticsdb)"
echo ""
echo "🔧 Commands:"
echo "   Status:        $SCRIPT_DIR/status-all-services.sh"
echo "   Stop:          $SCRIPT_DIR/stop-all-services.sh"
echo "   Logs:          docker logs -f <container-name>"
echo "                  (property-service | auth-service | post-service | analytics-service | price-service | news-service | ai-search-service | comment-service | kong-gateway)"
echo ""
echo "🌐 Frontend Commands:"
echo "   Start:   cd $ROOT_DIR/frontend && ./start-dev.sh"
echo "   Status:  cd $ROOT_DIR/frontend && ./status.sh"
echo "   Stop:    cd $ROOT_DIR/frontend && ./stop.sh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
