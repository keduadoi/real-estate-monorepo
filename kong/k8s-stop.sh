#!/bin/bash
# Script to stop Kong API Gateway

echo "🛑 Stopping Kong API Gateway"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Ask for confirmation
echo -e "${YELLOW}What do you want to stop?${NC}"
echo "1) Scale down Kong deployment only (keep PostgreSQL/Redis running)"
echo "2) Scale down all Kong components"
echo "3) Full cleanup (uninstall Helm release, delete namespace)"
echo ""
read -p "Choose option (1-3): " -n 1 -r
echo ""
echo ""

case $REPLY in
    1)
        echo "🔄 Scaling down Kong deployment..."
        kubectl scale deployment/kong --replicas=0 -n kong 2>/dev/null
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Kong scaled down to 0 replicas${NC}"
            echo "   PostgreSQL and Redis are still running"
            echo ""
            echo "To restart: kubectl scale deployment/kong --replicas=1 -n kong"
        else
            echo -e "${RED}❌ Failed to scale down Kong${NC}"
        fi

        # Kill port-forwards
        pkill -f "port-forward.*kong" 2>/dev/null
        ;;

    2)
        echo "🛑 Scaling down all Kong components..."

        kubectl scale deployment/kong --replicas=0 -n kong 2>/dev/null
        kubectl scale deployment/kong-redis --replicas=0 -n kong 2>/dev/null
        kubectl scale statefulset/kong-postgres --replicas=0 -n kong 2>/dev/null

        # Kill port-forwards
        pkill -f "port-forward.*kong" 2>/dev/null

        echo ""
        echo -e "${GREEN}✅ All Kong components scaled down${NC}"
        echo ""
        echo "To restart:"
        echo "   kubectl scale statefulset/kong-postgres --replicas=1 -n kong"
        echo "   kubectl scale deployment/kong-redis --replicas=1 -n kong"
        echo "   kubectl scale deployment/kong --replicas=1 -n kong"
        ;;

    3)
        echo -e "${YELLOW}⚠️  Full cleanup will remove:${NC}"
        echo "   - Kong Helm release"
        echo "   - Kong namespace and all resources"
        echo "   - Kong configuration data"
        echo ""
        read -p "Are you sure? (y/N): " -n 1 -r
        echo ""

        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo ""
            echo "🗑️  Performing full cleanup..."

            # Kill port-forwards
            pkill -f "port-forward.*kong" 2>/dev/null

            # Uninstall Helm release
            echo "   Uninstalling Helm release..."
            helm uninstall kong-gateway -n kong 2>/dev/null

            # Delete PVCs
            echo "   Deleting PVCs..."
            kubectl delete pvc --all -n kong 2>/dev/null

            # Delete namespace
            echo "   Deleting namespace..."
            kubectl delete namespace kong 2>/dev/null

            echo ""
            echo -e "${GREEN}✅ Full cleanup completed${NC}"
            echo -e "${RED}⚠️  Kong configuration has been deleted!${NC}"
            echo ""
            echo "To start fresh: ./k8s-start.sh"
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

echo "☸️  Kong (Kubernetes):"
kubectl get pods -n kong 2>/dev/null || echo "   Namespace not found"
echo ""
