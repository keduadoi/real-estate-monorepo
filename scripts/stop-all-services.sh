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
echo "1) Stop K8s services only (keep databases running)"
echo "2) Stop everything (K8s services + databases)"
echo "3) Full cleanup (uninstall Helm, stop DBs, preserve data)"
echo "4) Complete reset (delete everything including data)"
echo ""
read -p "Choose option (1-4): " -n 1 -r
echo ""
echo ""

case $REPLY in
    1)
        echo -e "${BLUE}━━━ Stopping Kubernetes Services ━━━${NC}"

        echo "   Scaling down deployments..."
        kubectl scale deployment/real-estate-backend-backend --replicas=0 -n real-estate 2>/dev/null
        kubectl scale deployment/auth-service --replicas=0 -n real-estate 2>/dev/null
        kubectl scale deployment/post-service --replicas=0 -n real-estate 2>/dev/null
        kubectl scale deployment/kong --replicas=0 -n kong 2>/dev/null

        echo "   Killing port-forwards..."
        pkill -f "port-forward" 2>/dev/null

        echo ""
        echo -e "${GREEN}✅ K8s services stopped (databases still running)${NC}"
        echo ""
        echo "To restart: kubectl scale deployment/<name> --replicas=1 -n <namespace>"
        ;;

    2)
        echo -e "${BLUE}━━━ Stopping All Services ━━━${NC}"

        echo "   Scaling down K8s deployments..."
        kubectl scale deployment/real-estate-backend-backend --replicas=0 -n real-estate 2>/dev/null
        kubectl scale deployment/auth-service --replicas=0 -n real-estate 2>/dev/null
        kubectl scale deployment/post-service --replicas=0 -n real-estate 2>/dev/null
        kubectl scale deployment/kong --replicas=0 -n kong 2>/dev/null

        echo "   Killing port-forwards..."
        pkill -f "port-forward" 2>/dev/null

        echo "   Stopping databases (preserving data)..."
        cd "$ROOT_DIR/backend" && docker-compose -f docker-compose-db.yml stop 2>/dev/null
        cd "$ROOT_DIR/auth-service" && docker-compose -f docker-compose-db.yml stop 2>/dev/null
        cd "$ROOT_DIR/post-service" && docker-compose -f docker-compose-db.yml stop 2>/dev/null

        echo ""
        echo -e "${GREEN}✅ All services stopped (data preserved)${NC}"
        echo ""
        echo "To restart: ./scripts/start-all-services.sh"
        ;;

    3)
        echo -e "${BLUE}━━━ Full Cleanup (Preserving Data) ━━━${NC}"

        echo "   Uninstalling Helm releases..."
        helm uninstall real-estate-backend -n real-estate 2>/dev/null
        helm uninstall auth-service -n real-estate 2>/dev/null
        helm uninstall post-service -n real-estate 2>/dev/null
        helm uninstall kong-gateway -n kong 2>/dev/null

        echo "   Killing port-forwards..."
        pkill -f "port-forward" 2>/dev/null

        echo "   Stopping databases (preserving data)..."
        cd "$ROOT_DIR/backend" && docker-compose -f docker-compose-db.yml stop 2>/dev/null
        cd "$ROOT_DIR/auth-service" && docker-compose -f docker-compose-db.yml stop 2>/dev/null
        cd "$ROOT_DIR/post-service" && docker-compose -f docker-compose-db.yml stop 2>/dev/null

        echo ""
        echo -e "${GREEN}✅ Cleanup completed (data preserved in Docker volumes)${NC}"
        echo ""
        echo "To start fresh: ./scripts/start-all-services.sh"
        ;;

    4)
        echo -e "${RED}⚠️  WARNING: This will delete ALL data!${NC}"
        echo "   - All Helm releases"
        echo "   - All Kubernetes resources"
        echo "   - All database data"
        echo ""
        read -p "Are you absolutely sure? Type 'DELETE' to confirm: " -r
        echo ""

        if [[ $REPLY == "DELETE" ]]; then
            echo -e "${BLUE}━━━ Complete Reset ━━━${NC}"

            echo "   Uninstalling Helm releases..."
            helm uninstall real-estate-backend -n real-estate 2>/dev/null
            helm uninstall auth-service -n real-estate 2>/dev/null
            helm uninstall post-service -n real-estate 2>/dev/null
            helm uninstall kong-gateway -n kong 2>/dev/null

            echo "   Deleting PVCs..."
            kubectl delete pvc --all -n real-estate 2>/dev/null
            kubectl delete pvc --all -n kong 2>/dev/null

            echo "   Deleting namespaces..."
            kubectl delete namespace real-estate 2>/dev/null
            kubectl delete namespace kong 2>/dev/null

            echo "   Killing port-forwards..."
            pkill -f "port-forward" 2>/dev/null

            echo "   Removing databases and volumes..."
            cd "$ROOT_DIR/backend" && docker-compose -f docker-compose-db.yml down -v 2>/dev/null
            cd "$ROOT_DIR/auth-service" && docker-compose -f docker-compose-db.yml down -v 2>/dev/null
            cd "$ROOT_DIR/post-service" && docker-compose -f docker-compose-db.yml down -v 2>/dev/null

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
for db in "real-estate-postgres-local" "auth-db" "post-db"; do
    if docker ps | grep -q "$db"; then
        echo -e "   $db: ${GREEN}Running${NC}"
    elif docker ps -a | grep -q "$db"; then
        echo -e "   $db: ${YELLOW}Stopped${NC}"
    else
        echo -e "   $db: ${RED}Not found${NC}"
    fi
done
echo ""

echo "☸️  Kubernetes Pods:"
kubectl get pods -n real-estate 2>/dev/null | head -10 || echo "   No pods in real-estate namespace"
kubectl get pods -n kong 2>/dev/null | head -5 || echo "   No pods in kong namespace"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔧 Commands:"
echo "   Start:  ./scripts/start-all-services.sh"
echo "   Status: ./scripts/status-all-services.sh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
