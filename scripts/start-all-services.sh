#!/bin/bash
# Master script to start all Real Estate services
# Services: PostgreSQL DBs (Docker) + Microservices (K8s) + Kong Gateway (K8s)

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

# Check Minikube
if ! minikube status > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Minikube is not running. Starting...${NC}"
    minikube start --memory=8192 --cpus=4
fi
echo -e "${GREEN}✅ Minikube is running${NC}"

# Point to Minikube Docker daemon
eval $(minikube docker-env)
echo -e "${GREEN}✅ Docker configured for Minikube${NC}"
echo ""

# ============================================================================
# STEP 2: Start All Databases (Docker)
# ============================================================================
echo -e "${BLUE}━━━ Step 2: Starting Databases (Docker) ━━━${NC}"

# Backend PostgreSQL
echo "   Starting backend PostgreSQL (port 5432)..."
cd "$ROOT_DIR/backend"
if ! docker ps | grep -q "real-estate-postgres-local"; then
    docker-compose -f docker-compose-db.yml up -d
fi
echo -e "   ${GREEN}✅ Backend DB ready${NC}"

# Auth PostgreSQL
echo "   Starting auth PostgreSQL (port 5433)..."
cd "$ROOT_DIR/auth-service"
if ! docker ps | grep -q "auth-db"; then
    docker-compose -f docker-compose-db.yml up -d
fi
echo -e "   ${GREEN}✅ Auth DB ready${NC}"

# Post PostgreSQL
echo "   Starting post PostgreSQL (port 5434)..."
cd "$ROOT_DIR/post-service"
if ! docker ps | grep -q "post-db"; then
    docker-compose -f docker-compose-db.yml up -d
fi
echo -e "   ${GREEN}✅ Post DB ready${NC}"

# Wait for DBs to be healthy
echo "   Waiting for databases to be healthy..."
sleep 5
echo ""

# ============================================================================
# STEP 3: Create Kubernetes Namespace
# ============================================================================
echo -e "${BLUE}━━━ Step 3: Setting up Kubernetes ━━━${NC}"
kubectl create namespace real-estate 2>/dev/null || true
kubectl create namespace kong 2>/dev/null || true
echo -e "${GREEN}✅ Namespaces ready${NC}"
echo ""

# ============================================================================
# STEP 4: Deploy Backend Service
# ============================================================================
echo -e "${BLUE}━━━ Step 4: Deploying Backend Service ━━━${NC}"
cd "$ROOT_DIR/backend"

if helm list -n real-estate 2>/dev/null | grep -q "real-estate-backend"; then
    echo "   Upgrading backend..."
    helm upgrade real-estate-backend ./helm/real-estate-backend \
      -f ./helm/real-estate-backend/values-local.yaml \
      -n real-estate --wait --timeout=120s
else
    echo "   Installing backend..."
    helm install real-estate-backend ./helm/real-estate-backend \
      -f ./helm/real-estate-backend/values-local.yaml \
      -n real-estate --wait --timeout=120s
fi
echo -e "${GREEN}✅ Backend deployed${NC}"
echo ""

# ============================================================================
# STEP 5: Deploy Auth Service
# ============================================================================
echo -e "${BLUE}━━━ Step 5: Deploying Auth Service ━━━${NC}"
cd "$ROOT_DIR/auth-service"

if helm list -n real-estate 2>/dev/null | grep -q "auth-service"; then
    echo "   Upgrading auth-service..."
    helm upgrade auth-service ./helm/auth-service \
      -f ./helm/auth-service/values-local.yaml \
      -n real-estate --wait --timeout=120s
else
    echo "   Installing auth-service..."
    helm install auth-service ./helm/auth-service \
      -f ./helm/auth-service/values-local.yaml \
      -n real-estate --wait --timeout=120s
fi
echo -e "${GREEN}✅ Auth service deployed${NC}"
echo ""

# ============================================================================
# STEP 6: Deploy Post Service
# ============================================================================
echo -e "${BLUE}━━━ Step 6: Deploying Post Service ━━━${NC}"
cd "$ROOT_DIR/post-service"

if helm list -n real-estate 2>/dev/null | grep -q "post-service"; then
    echo "   Upgrading post-service..."
    helm upgrade post-service ./helm/post-service \
      -f ./helm/post-service/values-local.yaml \
      -n real-estate --wait --timeout=120s
else
    echo "   Installing post-service..."
    helm install post-service ./helm/post-service \
      -f ./helm/post-service/values-local.yaml \
      -n real-estate --wait --timeout=120s
fi
echo -e "${GREEN}✅ Post service deployed${NC}"
echo ""

# ============================================================================
# STEP 7: Deploy Kong Gateway
# ============================================================================
echo -e "${BLUE}━━━ Step 7: Deploying Kong Gateway ━━━${NC}"
cd "$ROOT_DIR/kong"

if helm list -n kong 2>/dev/null | grep -q "kong-gateway"; then
    echo "   Upgrading kong-gateway..."
    helm upgrade kong-gateway ./helm/kong \
      -f ./helm/kong/values-local.yaml \
      -n kong --wait --timeout=180s
else
    echo "   Installing kong-gateway..."
    helm install kong-gateway ./helm/kong \
      -f ./helm/kong/values-local.yaml \
      -n kong --wait --timeout=180s
fi
echo -e "${GREEN}✅ Kong Gateway deployed${NC}"
echo ""

# ============================================================================
# STEP 8: Start Port Forwards
# ============================================================================
echo -e "${BLUE}━━━ Step 8: Starting Port Forwards ━━━${NC}"

# Kill existing port-forwards
pkill -f "port-forward.*8080:8080" 2>/dev/null
pkill -f "port-forward.*8081:8081" 2>/dev/null
pkill -f "port-forward.*8082:8082" 2>/dev/null
pkill -f "port-forward.*8000:80" 2>/dev/null
pkill -f "port-forward.*8001:8001" 2>/dev/null
sleep 2

# Start new port-forwards
kubectl port-forward svc/real-estate-backend-backend 8080:8080 -n real-estate > /dev/null 2>&1 &
echo "   Backend:      localhost:8080"

kubectl port-forward svc/auth-service 8081:8081 -n real-estate > /dev/null 2>&1 &
echo "   Auth Service: localhost:8081"

kubectl port-forward svc/post-service 8082:8082 -n real-estate > /dev/null 2>&1 &
echo "   Post Service: localhost:8082"

kubectl port-forward svc/kong-proxy 8000:80 -n kong > /dev/null 2>&1 &
echo "   Kong Proxy:   localhost:8000"

kubectl port-forward svc/kong-admin 8001:8001 -n kong > /dev/null 2>&1 &
echo "   Kong Admin:   localhost:8001"

sleep 3
echo -e "${GREEN}✅ Port forwards started${NC}"
echo ""

# ============================================================================
# STEP 9: Health Checks
# ============================================================================
echo -e "${BLUE}━━━ Step 9: Health Checks ━━━${NC}"

check_health() {
    local name=$1
    local url=$2
    if curl -s "$url" > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ $name is healthy${NC}"
        return 0
    else
        echo -e "   ${RED}❌ $name is not responding${NC}"
        return 1
    fi
}

check_health "Backend" "http://localhost:8080/actuator/health"
check_health "Auth Service" "http://localhost:8081/actuator/health"
check_health "Post Service" "http://localhost:8082/actuator/health"
check_health "Kong Gateway" "http://localhost:8000/api/properties"
echo ""

# ============================================================================
# STEP 10: Optional - Start Frontend
# ============================================================================
echo -e "${BLUE}━━━ Step 10: Frontend ━━━${NC}"
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
echo "   Backend API:   http://localhost:8080"
echo "   Auth Service:  http://localhost:8081"
echo "   Post Service:  http://localhost:8082"
echo ""
echo "🐘 Databases (Docker):"
echo "   Backend DB:    localhost:5432 (realestatedb)"
echo "   Auth DB:       localhost:5433 (authdb)"
echo "   Post DB:       localhost:5434 (postdb)"
echo ""
echo "🔧 Commands:"
echo "   Status:        $SCRIPT_DIR/status-all-services.sh"
echo "   Stop:          $SCRIPT_DIR/stop-all-services.sh"
echo "   Logs:          kubectl logs -f deployment/<service> -n real-estate"
echo ""
echo "🌐 Frontend Commands:"
echo "   Start:   cd $ROOT_DIR/frontend && ./start-dev.sh"
echo "   Status:  cd $ROOT_DIR/frontend && ./status.sh"
echo "   Stop:    cd $ROOT_DIR/frontend && ./stop.sh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
