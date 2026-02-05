#!/bin/bash
# Start Kong Gateway via Docker Compose (DB-less mode)

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${BLUE}Starting Kong Gateway (Docker Compose - DB-less mode)${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Docker is not running. Please start Docker Desktop.${NC}"
    exit 1
fi
echo -e "${GREEN}Docker is running${NC}"

# Check config file exists
if [ ! -f "$SCRIPT_DIR/config/kong-local.yaml" ]; then
    echo -e "${RED}Config file not found: $SCRIPT_DIR/config/kong-local.yaml${NC}"
    exit 1
fi

# Start Kong
echo "Starting Kong..."
cd "$SCRIPT_DIR"
docker compose up -d

# Wait for Kong to be healthy
echo ""
echo "Waiting for Kong to be healthy..."
MAX_RETRIES=30
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s http://localhost:8001/status > /dev/null 2>&1; then
        echo -e "${GREEN}Kong is healthy!${NC}"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
        echo -e "${RED}Kong did not become healthy in time.${NC}"
        echo "Check logs: docker compose -f $SCRIPT_DIR/docker-compose.yml logs"
        exit 1
    fi
    sleep 2
done

# Show status
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}Kong Gateway is running!${NC}"
echo ""
echo "  Proxy:  http://localhost:8000"
echo "  Admin:  http://localhost:8001"
echo ""

# Show route/service counts
SERVICES=$(curl -s http://localhost:8001/services | grep -o '"total":[0-9]*' | cut -d: -f2)
ROUTES=$(curl -s http://localhost:8001/routes | grep -o '"total":[0-9]*' | cut -d: -f2)
echo "  Services configured: ${SERVICES:-0}"
echo "  Routes configured:   ${ROUTES:-0}"
echo ""
echo "  Stop:   $SCRIPT_DIR/docker-stop.sh"
echo "  Status: $SCRIPT_DIR/docker-status.sh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
