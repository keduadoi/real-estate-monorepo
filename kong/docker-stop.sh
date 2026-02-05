#!/bin/bash
# Stop Kong Gateway (Docker Compose)

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Stopping Kong Gateway..."

cd "$SCRIPT_DIR"

if docker compose ps --quiet 2>/dev/null | grep -q .; then
    docker compose down
    echo -e "${GREEN}Kong Gateway stopped.${NC}"
else
    echo -e "${YELLOW}Kong Gateway is not running.${NC}"
fi
