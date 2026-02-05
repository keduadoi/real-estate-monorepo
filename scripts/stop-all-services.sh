#!/bin/bash
# Master script to stop all Real Estate services

echo "🛑 Stopping Real Estate Platform - All Services"
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

# Ask for confirmation
echo -e "${YELLOW}What do you want to stop?${NC}"
echo "1) Stop app services only (keep databases running)"
echo "2) Stop everything (app services + databases + Kong)"
echo "3) Full cleanup (remove containers, preserve data volumes)"
echo "4) Complete reset (remove everything including data)"
echo ""
read -p "Choose option (1-4): " -n 1 -r
echo ""
echo ""

case $REPLY in
    1)
        echo -e "${BLUE}━━━ Stopping App Services (keeping DBs) ━━━${NC}"

        echo "   Stopping backend app..."
        cd "$ROOT_DIR/backend" && docker compose stop backend 2>/dev/null

        echo "   Stopping auth-service app..."
        cd "$ROOT_DIR/auth-service" && docker compose stop auth-service 2>/dev/null

        echo "   Stopping post-service app..."
        cd "$ROOT_DIR/post-service" && docker compose stop post-service 2>/dev/null

        echo "   Stopping Kong Gateway..."
        cd "$ROOT_DIR/kong" && docker compose down 2>/dev/null

        echo ""
        echo -e "${GREEN}✅ App services stopped (databases still running)${NC}"
        echo ""
        echo "To restart: ./scripts/start-all-services.sh"
        ;;

    2)
        echo -e "${BLUE}━━━ Stopping All Services ━━━${NC}"

        echo "   Stopping backend..."
        cd "$ROOT_DIR/backend" && docker compose stop 2>/dev/null

        echo "   Stopping auth-service..."
        cd "$ROOT_DIR/auth-service" && docker compose stop 2>/dev/null

        echo "   Stopping post-service..."
        cd "$ROOT_DIR/post-service" && docker compose stop 2>/dev/null

        echo "   Stopping Kong Gateway..."
        cd "$ROOT_DIR/kong" && docker compose down 2>/dev/null

        echo ""
        echo -e "${GREEN}✅ All services stopped (containers and data preserved)${NC}"
        echo ""
        echo "To restart: ./scripts/start-all-services.sh"
        ;;

    3)
        echo -e "${BLUE}━━━ Full Cleanup (Preserving Data Volumes) ━━━${NC}"

        echo "   Removing backend containers..."
        cd "$ROOT_DIR/backend" && docker compose down 2>/dev/null

        echo "   Removing auth-service containers..."
        cd "$ROOT_DIR/auth-service" && docker compose down 2>/dev/null

        echo "   Removing post-service containers..."
        cd "$ROOT_DIR/post-service" && docker compose down 2>/dev/null

        echo "   Removing Kong Gateway..."
        cd "$ROOT_DIR/kong" && docker compose down 2>/dev/null

        echo ""
        echo -e "${GREEN}✅ Cleanup completed (data preserved in Docker volumes)${NC}"
        echo ""
        echo "To start fresh: ./scripts/start-all-services.sh"
        ;;

    4)
        echo -e "${RED}⚠️  WARNING: This will delete ALL data!${NC}"
        echo "   - All containers"
        echo "   - All database data (volumes)"
        echo "   - All built images"
        echo ""
        read -p "Are you absolutely sure? Type 'DELETE' to confirm: " -r
        echo ""

        if [[ $REPLY == "DELETE" ]]; then
            echo -e "${BLUE}━━━ Complete Reset ━━━${NC}"

            echo "   Removing backend containers and volumes..."
            cd "$ROOT_DIR/backend" && docker compose down -v 2>/dev/null

            echo "   Removing auth-service containers and volumes..."
            cd "$ROOT_DIR/auth-service" && docker compose down -v 2>/dev/null

            echo "   Removing post-service containers and volumes..."
            cd "$ROOT_DIR/post-service" && docker compose down -v 2>/dev/null

            echo "   Removing Kong Gateway..."
            cd "$ROOT_DIR/kong" && docker compose down 2>/dev/null

            echo ""
            echo -e "${GREEN}✅ Complete reset done${NC}"
            echo -e "${RED}⚠️  All data has been deleted!${NC}"
            echo ""
            echo "To start fresh: ./scripts/start-all-services.sh"
        else
            echo "❌ Reset cancelled"
        fi
        ;;

    *)
        echo "❌ Invalid option"
        exit 1
        ;;
esac

echo ""

# Show current status
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Current Status:"
echo ""

echo "🐘 Databases:"
for db in "real-estate-postgres" "auth-db" "post-db"; do
    if docker ps --format '{{.Names}}' | grep -q "^${db}$"; then
        echo -e "   $db: ${GREEN}Running${NC}"
    elif docker ps -a --format '{{.Names}}' | grep -q "^${db}$"; then
        echo -e "   $db: ${YELLOW}Stopped${NC}"
    else
        echo -e "   $db: ${RED}Not found${NC}"
    fi
done
echo ""

echo "🚀 App Services:"
for svc in "real-estate-backend" "auth-service" "post-service"; do
    if docker ps --format '{{.Names}}' | grep -q "^${svc}$"; then
        echo -e "   $svc: ${GREEN}Running${NC}"
    elif docker ps -a --format '{{.Names}}' | grep -q "^${svc}$"; then
        echo -e "   $svc: ${YELLOW}Stopped${NC}"
    else
        echo -e "   $svc: ${RED}Not found${NC}"
    fi
done
echo ""

echo "🦍 Kong Gateway:"
if docker ps --format '{{.Names}}' | grep -q "^kong-gateway$"; then
    echo -e "   kong-gateway: ${GREEN}Running${NC}"
else
    echo -e "   kong-gateway: ${RED}Stopped${NC}"
fi
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔧 Commands:"
echo "   Start:  ./scripts/start-all-services.sh"
echo "   Status: ./scripts/status-all-services.sh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
