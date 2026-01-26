#!/bin/bash
# Script to stop backend with external PostgreSQL

echo "🛑 Stopping Real Estate Backend Services"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Ask for confirmation
echo -e "${YELLOW}What do you want to stop?${NC}"
echo "1) Stop backend in Kubernetes only (keep PostgreSQL running)"
echo "2) Stop both backend and PostgreSQL"
echo "3) Full cleanup (uninstall Helm release, stop PostgreSQL, delete namespace)"
echo ""
read -p "Choose option (1-3): " -n 1 -r
echo ""
echo ""

case $REPLY in
    1)
        echo "🔄 Scaling down backend deployment..."
        kubectl scale deployment/real-estate-backend-backend --replicas=0 -n real-estate 2>/dev/null
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Backend scaled down to 0 replicas${NC}"
            echo "   PostgreSQL is still running in Docker"
            echo ""
            echo "To restart: kubectl scale deployment/real-estate-backend-backend --replicas=1 -n real-estate"
        else
            echo -e "${RED}❌ Failed to scale down backend${NC}"
        fi
        ;;
    2)
        echo "🛑 Stopping backend and PostgreSQL..."

        # Scale down backend
        echo "   Scaling down backend..."
        kubectl scale deployment/real-estate-backend-backend --replicas=0 -n real-estate 2>/dev/null

        # Stop PostgreSQL WITHOUT removing volumes
        echo "   Stopping PostgreSQL (preserving data)..."
        docker-compose -f docker-compose-db.yml stop

        echo ""
        echo -e "${GREEN}✅ Services stopped${NC}"
        echo "   Backend: Scaled to 0 replicas"
        echo "   PostgreSQL: Stopped (container stopped, volume preserved)"
        echo ""
        echo "ℹ️  Data is preserved:"
        echo "   - K8s PVCs still exist (backend uploads)"
        echo "   - Docker volume still exists (database data) ✅"
        echo ""
        echo "To restart: ./k8s-start-external.sh"
        ;;
    3)
        echo -e "${YELLOW}⚠️  Full cleanup will remove:${NC}"
        echo "   - Helm release"
        echo "   - Kubernetes namespace and all resources"
        echo "   - PostgreSQL container and data volume"
        echo ""
        read -p "Are you sure? (y/N): " -n 1 -r
        echo ""

        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo ""
            echo "🗑️  Performing full cleanup..."

            # Uninstall Helm release
            echo "   Uninstalling Helm release..."
            helm uninstall real-estate -n real-estate 2>/dev/null

            # Delete namespace
            echo "   Deleting namespace..."
            kubectl delete namespace real-estate 2>/dev/null

            # Stop and remove PostgreSQL (including volume)
            echo "   Removing PostgreSQL..."
            docker-compose -f docker-compose-db.yml down -v

            echo ""
            echo -e "${GREEN}✅ Full cleanup completed${NC}"
            echo ""
            echo -e "${RED}⚠️  All data has been deleted!${NC}"
            echo "   - Kubernetes resources removed"
            echo "   - PostgreSQL data volume removed"
            echo ""
            echo "To start fresh: ./k8s-start-external.sh"
        else
            echo ""
            echo "❌ Cleanup cancelled"
        fi
        ;;
    *)
        echo "❌ Invalid option"
        exit 1
        ;;
esac

echo ""

# Show current status
echo "📊 Current Status:"
echo ""

echo "🐘 PostgreSQL (Docker):"
if docker ps | grep -q "real-estate-postgres-local"; then
    echo -e "   ${GREEN}✅ Running${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}" | grep real-estate-postgres-local
else
    if docker ps -a | grep -q "real-estate-postgres-local"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
    else
        echo -e "   ${RED}❌ Not found${NC}"
    fi
fi
echo ""

echo "☸️  Backend (Kubernetes):"
kubectl get pods -n real-estate 2>/dev/null
if [ $? -ne 0 ]; then
    echo "   Namespace not found"
fi
echo ""

echo "💾 Data Volumes:"
echo "   Docker: docker volume ls | grep real-estate"
docker volume ls | grep postgres_data
echo "   K8s PVCs:"
kubectl get pvc -n real-estate 2>/dev/null || echo "   No PVCs found"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Quick Commands:"
echo "  Start:   ./k8s-start-external.sh"
echo "  Status:  ./k8s-status-external.sh"
echo "  Logs:    kubectl logs -f deployment/real-estate-backend-backend -n real-estate"
echo "           docker logs -f real-estate-postgres-local"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
