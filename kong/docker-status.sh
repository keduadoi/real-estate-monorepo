#!/bin/bash
# Check Kong Gateway status (Docker Compose)

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${BLUE}Kong Gateway Status (Docker)${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check container status
cd "$SCRIPT_DIR"
if docker ps --filter "name=kong-gateway" --format '{{.Status}}' 2>/dev/null | grep -q "Up"; then
    CONTAINER_STATUS=$(docker ps --filter "name=kong-gateway" --format '{{.Status}}')
    echo -e "Container:  ${GREEN}Running${NC} ($CONTAINER_STATUS)"
else
    if docker ps -a --filter "name=kong-gateway" --format '{{.Status}}' 2>/dev/null | grep -q .; then
        echo -e "Container:  ${YELLOW}Stopped${NC}"
    else
        echo -e "Container:  ${RED}Not found${NC}"
    fi
    echo ""
    echo "Start with: $SCRIPT_DIR/docker-start.sh"
    exit 0
fi

# Health check via admin API
echo ""
if curl -s http://localhost:8001/status > /dev/null 2>&1; then
    echo -e "Admin API:  ${GREEN}Healthy${NC}"

    # Show service/route counts
    SERVICES=$(curl -s http://localhost:8001/services | grep -o '"total":[0-9]*' | cut -d: -f2)
    ROUTES=$(curl -s http://localhost:8001/routes | grep -o '"total":[0-9]*' | cut -d: -f2)
    echo ""
    echo "  Services: ${SERVICES:-0}"
    echo "  Routes:   ${ROUTES:-0}"
else
    echo -e "Admin API:  ${RED}Not responding${NC}"
fi

echo ""
echo "  Proxy:  http://localhost:8000"
echo "  Admin:  http://localhost:8001"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
