#!/bin/bash
# Script to stop post-service with external PostgreSQL

echo "🛑 Stopping Post Service"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Ask for confirmation
echo -e "${YELLOW}What do you want to stop?${NC}"
echo "1) Stop post-service in Kubernetes only (keep PostgreSQL running)"
echo "2) Stop both post-service and PostgreSQL"
echo "3) Full cleanup (uninstall Helm release, stop PostgreSQL, delete data)"
echo ""
read -p "Choose option (1-3): " -n 1 -r
echo ""
echo ""

case $REPLY in
    1)
        echo "🔄 Scaling down post-service deployment..."
        kubectl scale deployment/post-service --replicas=0 -n real-estate 2>/dev/null
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Post service scaled down to 0 replicas${NC}"
            echo "   PostgreSQL is still running in Docker"
            echo ""
            echo "To restart: kubectl scale deployment/post-service --replicas=1 -n real-estate"
        else
            echo -e "${RED}❌ Failed to scale down post-service${NC}"
        fi
        ;;
    2)
        echo "🛑 Stopping post-service and PostgreSQL..."

        echo "   Scaling down post-service..."
        kubectl scale deployment/post-service --replicas=0 -n real-estate 2>/dev/null

        echo "   Stopping PostgreSQL (preserving data)..."
        docker-compose -f docker-compose-db.yml stop

        echo ""
        echo -e "${GREEN}✅ Services stopped${NC}"
        echo "   Post service: Scaled to 0 replicas"
        echo "   PostgreSQL: Stopped (data preserved)"
        echo ""
        echo "To restart: ./k8s-start-external.sh"
        ;;
    3)
        echo -e "${YELLOW}⚠️  Full cleanup will remove:${NC}"
        echo "   - Helm release"
        echo "   - PostgreSQL container and data volume"
        echo ""
        read -p "Are you sure? (y/N): " -n 1 -r
        echo ""

        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo ""
            echo "🗑️  Performing full cleanup..."

            echo "   Uninstalling Helm release..."
            helm uninstall post-service -n real-estate 2>/dev/null

            echo "   Removing PostgreSQL..."
            docker-compose -f docker-compose-db.yml down -v

            echo ""
            echo -e "${GREEN}✅ Full cleanup completed${NC}"
            echo -e "${RED}⚠️  All post data has been deleted!${NC}"
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
if docker ps | grep -q "post-db"; then
    echo -e "   ${GREEN}✅ Running${NC}"
else
    if docker ps -a | grep -q "post-db"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
    else
        echo -e "   ${RED}❌ Not found${NC}"
    fi
fi
echo ""

echo "☸️  Post Service (Kubernetes):"
kubectl get pods -n real-estate -l app.kubernetes.io/name=post-service 2>/dev/null || echo "   No pods found"
echo ""
